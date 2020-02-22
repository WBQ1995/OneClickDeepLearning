package acceler.ocdl.controller;


import acceler.ocdl.dto.UploadDto;
import acceler.ocdl.entity.Project;
import acceler.ocdl.entity.Template;
import acceler.ocdl.entity.TemplateCategory;
import acceler.ocdl.service.ProjectService;
import acceler.ocdl.service.TemplateService;
import acceler.ocdl.dto.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static acceler.ocdl.dto.Response.getBuilder;

@RestController
@RequestMapping(path = "/rest/template")
public final class TemplateController {

    @Autowired
    private TemplateService templateService;

    @Autowired
    private ProjectService projectService;

//    @ResponseBody
//    @RequestMapping(path = "/file", method = RequestMethod.GET)
//    public final Response getTemplateFiles(HttpServletRequest request){
//        Response.Builder responseBuilder = getBuilder();
//
//        Map<String,List<String>> result;
//        try{
//            result =  templateService.getTemplatesList();
//            responseBuilder.setCode(Response.Code.SUCCESS);
//            responseBuilder.setData(result);
//        }catch (Exception e){
//            responseBuilder.setCode(Response.Code.ERROR);
//            responseBuilder.setMessage(e.getMessage());
//        }
//        return responseBuilder.build();
//    }
//
//    @ResponseBody
//    @RequestMapping(path = "/code", method = RequestMethod.GET)
//    public final Response getTemplateCode(@QueryParam("name")String name, @QueryParam("type")String type){
//        List<String> templates = new ArrayList <String>();
//        templates = templateService.getCode(name,type);
//
//        return Response.getBuilder()
//                .setCode(Response.Code.SUCCESS)
//                .setData(templates)
//                .build();
//    }


    @RequestMapping(path = "/category", method = RequestMethod.GET)
    public Response getCategory(HttpServletRequest request){
        Response.Builder responseBuilder = getBuilder();

        Project project = (Project)request.getAttribute("PROJECT");
        TemplateCategory category = templateService.getProjectCategory(project);

        return responseBuilder
                .setCode(Response.Code.SUCCESS)
                .setData(category)
                .build();
    }


    @RequestMapping(path = "/category", method = RequestMethod.POST)
    public Response saveCategory(@RequestBody TemplateCategory templateCategory,
                                 HttpServletRequest request) {

        Response.Builder responseBuilder = getBuilder();

        Project project = (Project)request.getAttribute("PROJECT");
        templateCategory.setProject(project);
        TemplateCategory category = templateService.saveCategory(templateCategory);

        return responseBuilder
                .setCode(Response.Code.SUCCESS)
                .setData(category)
                .build();
    }


    @RequestMapping(path = "/category", method = RequestMethod.DELETE)
    public Response deleteCategory(@RequestBody TemplateCategory templateCategory) {
        Response.Builder responseBuilder = getBuilder();

        Boolean success = templateService.deleteCategory(templateCategory);

        return responseBuilder
                .setCode(Response.Code.SUCCESS)
                .setData(success)
                .build();
    }


    @RequestMapping(path = "/get", method = RequestMethod.POST)
    public Response getTemplate(@RequestBody Template template,
                                @RequestParam(value = "page", required = false, defaultValue = "0") int page ,
                                @RequestParam(value = "size", required = false, defaultValue = "10") int size){

        Response.Builder responseBuilder = getBuilder();

        Page<Template> templatePage = templateService.getTemplate(template, page, size);

        return responseBuilder
                .setCode(Response.Code.SUCCESS)
                .setData(templatePage)
                .build();
    }


    @RequestMapping(path = "/", method = RequestMethod.POST)
    public Response uploadTemplate(HttpServletRequest request,
                                   @RequestBody UploadDto uploadDto) {
        Response.Builder responseBuilder = getBuilder();

        Project project = (Project)request.getAttribute("PROJECT");
        Template templateInDb = templateService.uploadTemplate(project, uploadDto.getSrc(), uploadDto.getCategory());

        return responseBuilder
                .setCode(Response.Code.SUCCESS)
                .setData(templateInDb)
                .build();
    }


    @RequestMapping(path = "/", method = RequestMethod.GET)
    public Response downloadTemplate(@RequestParam(name = "refid") String refId,
                                     HttpServletRequest request) {
        Response.Builder responseBuilder = getBuilder();

        Project project = (Project)request.getAttribute("PROJECT");
        boolean success = templateService.downloadTemplate(refId, project);

        return responseBuilder
                .setCode(Response.Code.SUCCESS)
                .setData(success)
                .build();
    }

    @RequestMapping(path = "/", method = RequestMethod.DELETE)
    public Response batchDeleteTemplate(@RequestBody List<Template> templates) {
        Response.Builder responseBuilder = getBuilder();

        Boolean success = templateService.batchDeleteTemplate(templates);

        return responseBuilder
                .setCode(Response.Code.SUCCESS)
                .setData(success)
                .build();
    }





}
