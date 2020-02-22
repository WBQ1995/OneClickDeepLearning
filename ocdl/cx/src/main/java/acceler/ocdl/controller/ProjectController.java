package acceler.ocdl.controller;

import acceler.ocdl.dto.Response;
import acceler.ocdl.dto.UserRoleDto;
import acceler.ocdl.entity.*;
import acceler.ocdl.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static acceler.ocdl.dto.Response.getBuilder;

@RestController
@RequestMapping(path = "/rest/project")
public class ProjectController {

    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);

    @Autowired
    private ProjectService projectService;

    @Autowired
    private AlgorithmService algorithmService;

    @Autowired
    private SuffixService suffixService;

    @Autowired
    protected UserService userService;

    @Autowired
    private ProjectDataService projectDataService;

    @RequestMapping(path = "/algorithm/get", method = RequestMethod.POST)
    public Response getAlgorithm(@RequestBody Algorithm algorithm,
                                       @RequestParam(value = "page", required = false, defaultValue = "0") int page ,
                                       @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        Response.Builder responseBuilder = getBuilder();

        Page<Algorithm> algorithmPage = algorithmService.getAlgorithm(algorithm, page, size);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(algorithmPage)
                .build();
    }

    @RequestMapping(path = "/algorithm", method = RequestMethod.POST)
    public Response saveAlgorithm(@RequestBody Algorithm algorithm) {

        Response.Builder responseBuilder = getBuilder();

        Algorithm algorithmInDb = algorithmService.saveAlgorithm(algorithm);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(algorithmInDb)
                .build();
    }


    @RequestMapping(path = "/algorithm", method = RequestMethod.DELETE)
    public Response batchDeleteAlgorithm(@RequestBody List<Algorithm> algorithms) {

        Response.Builder responseBuilder = getBuilder();

        boolean success = algorithmService.batchDeleteAlgorithm(algorithms);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(success)
                .build();
    }



//    @ResponseBody
//    @RequestMapping(path = "/latest/{algorithm}", method = RequestMethod.GET)
//    public final Response getLatestModelName(@PathVariable String algorithm) {
//
//        Response.Builder responseBuilder = getBuilder();
//
//        String latestModelName = algorithmService.getLatestModelName(algorithm);
//
//        responseBuilder.setCode(Response.Code.SUCCESS)
//                .setData(latestModelName);
//        return responseBuilder.build();
//    }

    @RequestMapping(path = "", method = RequestMethod.GET)
    public final Response getProjectConfig(@RequestParam(value = "id") Long id) {

        Response.Builder responseBuilder = Response.getBuilder();

        Project project = projectService.getProject(id);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(project)
                .build();
    }


    @RequestMapping(path = "/config", method = RequestMethod.POST)
    public Response saveProject(@RequestBody Project project, HttpServletRequest request) {

        Response.Builder responseBuilder = Response.getBuilder();

        User user = (User) request.getAttribute("CURRENT_USER");
        Project projectInDb = projectService.saveProject(project, user);
        //algorithmService.updateAlgorithmList(updatedProjectConfig.getAlgorithmStrList(), updatedProjectConfig.getForceRemoved());

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(projectInDb)
                .build();
    }


    @RequestMapping(path = "", method = RequestMethod.DELETE)
    public Response deleteProject(@RequestBody Project project) {

        Response.Builder responseBuilder = getBuilder();

        boolean success = projectService.deleteProject(project.getId());

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(success)
                .build();
    }


    @RequestMapping(path = "/suffix/get", method = RequestMethod.POST)
    public Response getSuffix(@RequestBody Suffix suffix,
                                 @RequestParam(value = "page", required = false, defaultValue = "0") int page ,
                                 @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        Response.Builder responseBuilder = getBuilder();

        Page<Suffix> suffixPage = suffixService.getSuffix(suffix, page, size);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(suffixPage)
                .build();
    }

    @RequestMapping(path = "/suffix", method = RequestMethod.POST)
    public Response createSuffix(@RequestBody Suffix suffix) {

        Response.Builder responseBuilder = getBuilder();

        Suffix suffixInDb = suffixService.createSuffix(suffix);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(suffixInDb)
                .build();
    }


    @RequestMapping(path = "/suffix", method = RequestMethod.DELETE)
    public Response batchDeleteSuffix(@RequestBody List<Suffix> suffixes) {

        Response.Builder responseBuilder = getBuilder();

        boolean success = suffixService.batchDeleteSuffix(suffixes);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(success)
                .build();
    }


    @RequestMapping(path = "/projectdata/get", method = RequestMethod.POST)
    public Response getProjectData(@RequestBody ProjectData projectData,
                              @RequestParam(value = "page", required = false, defaultValue = "0") int page ,
                              @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        Response.Builder responseBuilder = getBuilder();

        Page<ProjectData> projectDataPage = projectDataService.getProjectData(projectData, page, size);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(projectDataPage)
                .build();
    }

    // TODO: file
    @RequestMapping(path = "/projectdata", method = RequestMethod.POST)
    public Response uploadProjectData(HttpServletRequest request,
                                      @RequestParam(name = "srcpath") String srcPath) {

        Response.Builder responseBuilder = getBuilder();

        Project project = (Project) request.getAttribute("PROJECT");
        ProjectData projectData = projectDataService.uploadProjectData(project, srcPath);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(projectData)
                .build();
    }

    // TODO: file
    @RequestMapping(path = "/projectdata", method = RequestMethod.GET)
    public Response downloadProjectData(HttpServletRequest request, @RequestParam(name = "refid") String refId) {

        Response.Builder responseBuilder = getBuilder();

        Project project = (Project) request.getAttribute("PROJECT");
        boolean success = projectDataService.downloadProjectData(refId, project);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(success)
                .build();
    }


    @RequestMapping(path = "/projectdata", method = RequestMethod.DELETE)
    public Response batchDeleteProjectData(@RequestBody List<ProjectData> projectDatas,
                                           HttpServletRequest request) {

        Response.Builder responseBuilder = getBuilder();

        Project project = (Project) request.getAttribute("PROJECT");
        boolean success = projectDataService.batchDeleteProjectData(projectDatas, project);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(success)
                .build();
    }


    @RequestMapping(path = "/coop", method = RequestMethod.POST)
    public Response addCooperator(@RequestBody UserRoleDto userRoleDto,
                                  HttpServletRequest request) {

        Response.Builder responseBuilder = getBuilder();

        Project project = (Project) request.getAttribute("PROJECT");
        RUserRole rUserRole = userService.addRole(userRoleDto.getUser(), userRoleDto.getRole(), project);

        return responseBuilder.setCode(Response.Code.SUCCESS)
                .setData(rUserRole)
                .build();
    }




//    @RequestMapping(path = "/config/name", method = RequestMethod.PUT)
//    @ResponseBody
//    public Response updateProjectNames(@RequestBody Map<String, String> projectName) {
//
//        Response.Builder responseBuilder = Response.getBuilder();
//        String name = projectName.get("name");
//
//        if (!StringUtil.isNullOrEmpty(name)) {
//            Project project = new Project();
//            project.setProjectName(projectName.get("name"));
//            projectService.updateProjectConfiguration(project);
//            responseBuilder.setCode(Response.Code.SUCCESS)
//                    .setData(projectName);
//        } else {
//            responseBuilder.setCode(Response.Code.ERROR).setMessage("ProjectName can not be empty");
//        }
//        return responseBuilder.build();
//    }

}
