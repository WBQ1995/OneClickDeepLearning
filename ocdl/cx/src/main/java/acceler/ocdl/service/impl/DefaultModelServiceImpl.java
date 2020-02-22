package acceler.ocdl.service.impl;

import acceler.ocdl.CONSTANTS;
import acceler.ocdl.dto.ModelDto;
import acceler.ocdl.exception.HdfsException;
import acceler.ocdl.exception.NotFoundException;
import acceler.ocdl.exception.OcdlException;
import acceler.ocdl.model.*;
import acceler.ocdl.service.HdfsService;
import acceler.ocdl.service.MessageQueueService;
import acceler.ocdl.service.ModelService;
import acceler.ocdl.service.StorageService;
import acceler.ocdl.utils.TimeUtil;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;

import static org.apache.commons.io.FileUtils.directoryContains;


@DependsOn({"storageLoader"})
public class DefaultModelServiceImpl  {

    private static final Logger log = Logger.getLogger(DefaultModelServiceImpl.class);

    @Autowired
    private HdfsService hdfsService;

    @Autowired
    private StorageService storageService;

    @Autowired
    private MessageQueueService messageQueueService;

    @Value("${S3.server.bucketName}")
    private String bucketName;
    @Value("${APPLICATIONS_DIR.USER_SPACE}")
    private String applicationsDirUserSpace;
    @Value("${APPLICATIONS_DIR.STAGE_SPACE}")
    private String getApplicationsDirStageSpace;
    @Value("${KAFKA.TOPIC}")
    private String kafkaTopic;



    public Map<String, Integer> initModelToStage(InnerUser innerUser) {

        Map<String, Integer> initRecords = new HashMap<>();
        initRecords.put("finded", 0);
        initRecords.put("successUpload", 0);
        initRecords.put("failUpload", 0);

        final String userSpaceName = applicationsDirUserSpace + CONSTANTS.NAME_FORMAT.USER_SPACE.replace("{userId}", String.valueOf(innerUser.getUserId()));
        log.info(userSpaceName);
        System.out.println(userSpaceName);
        final File userSpace = new File(userSpaceName);

        if (userSpace.isDirectory()) {
            File[] files = userSpace.listFiles();
            List<String> suffixes = Arrays.asList(Project.getModelFileSuffixesInStorage());

            // HDFS upload error occures
            if (files == null) {
                log.error(String.format("fail in reading directory: %s", userSpaceName));
                throw new OcdlException(String.format("fail in reading directory: %s", userSpaceName));
            }

            int current = 0;

            for (; current < files.length; current++) {
                File f = files[current];

                if (!f.isDirectory() && isModelFile(suffixes, f.getName())) {
                    initRecords.put("finded", (int)initRecords.get("finded")+1);

                    try {
                        Long modelId = Model.generateModelId();

                        String suffix = f.getName().substring(f.getName().lastIndexOf(".")+1);

                        String stagedFilePath = Paths.get(getApplicationsDirStageSpace,CONSTANTS.NAME_FORMAT.STAGED_MODEL.replace("{modelId}",
                                modelId.toString()).replace("{suffix}", suffix)).toString();

                        hdfsService.uploadFile(new Path(f.getPath()), new Path(stagedFilePath));
                        persistNewModel(f, modelId, suffix, innerUser.getUserId());
                        initRecords.put("successUpload", (int)initRecords.get("successUpload")+1);

                    } catch (HdfsException e) {
                        initRecords.put("failUpload", (int) initRecords.get("failUpload") + 1);
                        log.error(String.format("Fail to init %s to new model", f.getName()));
                        continue;
                    }
                }
            }
        } else {
            throw new NotFoundException(String.format("%s, Fail to find user space.", innerUser.getUserId()));
        }
        return initRecords;
    }

    private void persistNewModel(File modelFile, Long modelId, String suffix, Long ownerId) {
        NewModel model = new NewModel();
        model.setModelId(modelId);
        model.setSuffix(suffix);
        model.setName(modelFile.getName());
        model.setCommitTime(TimeUtil.currentTime());
        model.setOwnerId(ownerId);
        model.setLastOperator(ownerId);
        log.info("when persist new model, owner id is " + ownerId);
        System.out.println("when persist new model, owner id is " + ownerId);
        NewModel.addToStorage(model);
    }


    public void approveModel(NewModel model, String algorithmName, Algorithm.UpgradeVersion version, String comments, Long lastOperatorId) {
        checkIfNewModelExist(model);
        Algorithm algorithm = Algorithm.getAlgorithmByName(algorithmName).orElseThrow(() -> (new NotFoundException(String.format("Not found algorithm: %s", algorithmName))));

        ApprovedModel approvedModel = algorithm.approveModel(model, version, comments, lastOperatorId);

        log.debug("after approved model, the ower id is: " + approvedModel.getOwnerId());
        algorithm.persistApprovalModel(approvedModel);
        NewModel.removeFromStorage(model.getModelId());
    }


    private void checkIfNewModelExist(NewModel model) {
        if (!NewModel.existNewModel(model)) {
            throw new NotFoundException("Not Found model:" + model.getName());
        }
    }



    public void rejectModel(NewModel model, String comments, Long lastOperatorId) {
        checkIfNewModelExist(model);
        Date current = TimeUtil.currentTime();
        RejectedModel.addToStorage(model.convertToRejectedModel(comments, lastOperatorId));
        NewModel.removeFromStorage(model.getModelId());
    }


    public void undo(Model model, String comments, Long lastOperatorId) {
        if (model instanceof ApprovedModel && !Algorithm.existApprovalModel((ApprovedModel) model)) {
            throw new NotFoundException("model not found");
        }

        if (model instanceof RejectedModel && !RejectedModel.existRejectedModel((RejectedModel) model)) {
            throw new NotFoundException("model not found");
        }

        if (model instanceof NewModel) {
            throw new RuntimeException("can not undo new model");
        }

        NewModel newModel;

        if (model instanceof ApprovedModel) {
            ApprovedModel approvedModel = (ApprovedModel) model;
            Algorithm algorithm = Algorithm.getAlgorithmOfApprovedModel(approvedModel);
            Algorithm.removeApprovedModelFromAlgorithm(algorithm.getAlgorithmName(), approvedModel);
            newModel = approvedModel.convertToNewModel(comments, lastOperatorId);
        } else {
            RejectedModel.removeFromStorage(model.getModelId());
            newModel = ((RejectedModel) model).convertToNewModel(comments, lastOperatorId);
        }

        NewModel.addToStorage(newModel);
    }



    public ModelDto[] getModelsByStatus(Model.Status status) {

        List<ModelDto> modelDtoList = null;

        switch (status) {
            case NEW:
                Model[] newModels = NewModel.getAllNewModels();
                modelDtoList = convertModelsToModelDtoList(newModels);
                break;
            case REJECTED:
                Model[] rejectedModels = RejectedModel.getAllRejectedModels();
                modelDtoList = convertModelsToModelDtoList(rejectedModels);
                break;
            case APPROVED:
                Map<String, Model[]> approvedModelMap = Algorithm.getAllAlgorithmAndModels();
                modelDtoList = convertModelsToModelDtoList(approvedModelMap);
                break;
        }
        return modelDtoList.toArray(new ModelDto[modelDtoList.size()]);
    }

    /**
     * Convert NewModel[] and RejectedModel[] to ModelDto[]
     *
     * @param modelArray
     * @return
     */
    private List<ModelDto> convertModelsToModelDtoList(Model[] modelArray) {

        List<ModelDto> modelDtoList = new ArrayList<>();

        for (Model model : modelArray) {
            ModelDto modelDto = model.convertToModelDto(model);
            modelDtoList.add(modelDto);
        }

        return modelDtoList;
    }


    /**
     * Convert Map<AlgorithmName, ApprovedModel[]> to ModelDto[]
     *
     * @param modelMap
     * @return
     */
    private List<ModelDto> convertModelsToModelDtoList(Map<String, Model[]> modelMap) {

        List<ModelDto> modelDtoList = new ArrayList<>();
        for (String algorithmName : modelMap.keySet()) {

            List<ModelDto> modelDtos = convertModelsToModelDtoList(modelMap.get(algorithmName));
            modelDtoList.addAll(modelDtos);
        }
        return modelDtoList;
    }


    private boolean isModelFile(List<String> modelSuffixes, String fileName) {
        return modelSuffixes.stream().anyMatch(s -> fileName.trim().endsWith(s));
    }


    private Optional<File> existModelFile(ApprovedModel model, InnerUser innerUser) {
        File modelFile = null;

        final String userSpaceName = applicationsDirUserSpace + CONSTANTS.NAME_FORMAT.USER_SPACE.replace("{userId}", String.valueOf(innerUser.getUserId()));
        final File userSpace = new File(userSpaceName);

        if (userSpace.isDirectory()) {
            modelFile = Paths.get(userSpaceName, model.getModelFileName()).toFile();
            boolean containsModel;
            try{
                containsModel = directoryContains(userSpace, modelFile);
            } catch (IOException e) {
                throw new OcdlException("Fail to check the model file existence.");
            }

            if (!containsModel) { modelFile = null; }
        } else {
            throw new NotFoundException(String.format("%s, Fail to find user space.", innerUser.getUserId()));
        }

        return Optional.ofNullable(modelFile);
    }



    public void release(ApprovedModel model, InnerUser innerUser) {

        File modelFile = existModelFile(model, innerUser)
                .orElseThrow(() -> new NotFoundException(String.format("%s, Fail to find model file, Please download first.", innerUser.getUserId())));

        // upload file to AWS S3
        String publishedModelName = CONSTANTS.NAME_FORMAT.RELEASE_MODEL.replace("{algorithm}", Algorithm.getAlgorithmOfApprovedModel(model).getAlgorithmName())
                .replace("{release_version}", model.getReleasedVersion().toString())
                .replace("{cached_version}", model.getCachedVersion().toString())
                .replace("{suffix}", model.getSuffix());

        storageService.uploadObject(bucketName, publishedModelName, modelFile);
        //send kafka message
        String message = CONSTANTS.KAFKA.MESSAGE.replace("{publishedModelName}", publishedModelName).replace("{modelUrl}",storageService.getObkectUrl(bucketName, publishedModelName));
        System.out.println("+++++++++++++++++++++++++++++++++++++++");
        System.out.println(message);
        messageQueueService.send(kafkaTopic, message);

        Algorithm algorithm = Algorithm.getAlgorithmOfApprovedModel(model);
        System.out.println(algorithm.getAlgorithmName());
        //update release status in Approved model
        algorithm.updateSingleApprovedModel(model, (ApprovedModel approvedModel) -> approvedModel.setStatus(Model.Status.RELEASED) );
        //update algorithm version
        algorithm.persistAlgorithmVersion(model);
        //update expected version of others approve model
        //algorithm.updateApprovedModels();
    }


    public Map<String, List<ModelDto>> getModelListByUser(long userId) {

        Map<String, List<ModelDto>> modelMap = new HashMap<>();

        List<ModelDto> personalEvent = new ArrayList<>();
        personalEvent.addAll(getNewModelsByUser(userId));
        personalEvent.addAll(getRejectModelsByUser(userId));
        TimeUtil.addNewFlag(personalEvent);
        Collections.sort(personalEvent);
        modelMap.put(CONSTANTS.EVENT.PERSONAL_EVENT, personalEvent);

        List<ModelDto> globalEvent = new ArrayList<>();
        globalEvent.addAll(Arrays.asList(getModelsByStatus(Model.Status.APPROVED)));
        TimeUtil.addNewFlag(globalEvent);
        Collections.sort(globalEvent);
        modelMap.put(CONSTANTS.EVENT.GLOBAL_EVENT, globalEvent);

        return modelMap;
    }




    private List<ModelDto> getNewModelsByUser(long userId) {

        Model[] newModels = NewModel.getAllNewModelsByUser(userId);
        List<ModelDto> modelDtoList = convertModelsToModelDtoList(newModels);

        return modelDtoList;
    }


    private List<ModelDto> getRejectModelsByUser(long userId) {

        Model[] newModels = RejectedModel.getAllRejectedModelsByUser(userId);
        List<ModelDto> modelDtoList = convertModelsToModelDtoList(newModels);

        return modelDtoList;
    }
}
