package acceler.ocdl.service.impl;

import acceler.ocdl.model.Project;
import acceler.ocdl.service.ProjectService;
import io.netty.util.internal.StringUtil;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;


@DependsOn({"storageLoader"})
public class DefaultProjectService {


    public Project updateProjectConfiguration(Project updatedProjectInfo) {

        Project currentProjectData = Project.getProjectInStorage();

        // get new projectData
        if (!StringUtil.isNullOrEmpty(updatedProjectInfo.getProjectName())) {
            currentProjectData.setProjectName(updatedProjectInfo.getProjectName());
        }

        if (!StringUtil.isNullOrEmpty(updatedProjectInfo.getDataPath())) {
            currentProjectData.setDataPath(updatedProjectInfo.getDataPath());
        }

        if (!StringUtil.isNullOrEmpty(updatedProjectInfo.getK8MasterUri())) {
            currentProjectData.setK8MasterUri(updatedProjectInfo.getK8MasterUri());
        }

        if (!StringUtil.isNullOrEmpty(updatedProjectInfo.getTemplatePath())) {
            currentProjectData.setTemplatePath(updatedProjectInfo.getTemplatePath());
        }

        if (!StringUtil.isNullOrEmpty(updatedProjectInfo.getDescription())) {
            currentProjectData.setDescription(updatedProjectInfo.getDescription());
        }

        if (updatedProjectInfo.getSuffixes() != null && updatedProjectInfo.getSuffixes().size() != 0) {
            currentProjectData.setSuffixes(updatedProjectInfo.getSuffixes());
        }

        Project.setProjectDataStorage(currentProjectData);

        return currentProjectData;
    }


    public Project getProjectConfiguration() {
        return Project.getProjectInStorage();

    }
}
