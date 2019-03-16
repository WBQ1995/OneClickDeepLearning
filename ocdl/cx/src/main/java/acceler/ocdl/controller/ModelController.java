package acceler.ocdl.controller;

import acceler.ocdl.model.User;
import acceler.ocdl.service.ModelService;
import acceler.ocdl.dto.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping(path = "/rest/model")
public final class ModelController {

    @Autowired
    private ModelService modelService;

    @ResponseBody
    @RequestMapping(method = RequestMethod.PUT)
    public final Response queryPushModels(HttpServletRequest request) {

        User user = (User) request.getAttribute("CURRENT_USER");

        if(modelService.copyModels(user)) {
            return Response.getBuilder()
                    .setCode(Response.Code.SUCCESS)
                    .setData("copy succeeded")
                    .build();
        } else{
            return Response.getBuilder()
                    .setCode(Response.Code.ERROR)
                    .setMessage("copy failed")
                    .build();
        }
    }
}
