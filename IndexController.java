package com.example.blog.controller.home;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.controller.common.BaseController;
import com.example.blog.dto.PostQueryCondition;
import com.example.blog.dto.QueryCondition;
import com.example.blog.entity.*;
import com.example.blog.enums.PostTypeEnum;
import com.example.blog.service.*;
import com.example.blog.util.PageUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author 言曌
 * @date 2020/3/9 11:00 上午
 */

@Controller
public class IndexController extends BaseController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;


    /**
     * 最新文章
     *
     * @param pageNumber
     * @param pageSize
     * @param sort
     * @param order
     * @param model
     * @return
     */
    @RequestMapping(value = {"/", "/post"}, method = RequestMethod.GET)
    public String index(@RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                        @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                        @RequestParam(value = "sort", defaultValue = "isSticky desc, createTime") String sort,
                        @RequestParam(value = "order", defaultValue = "desc") String order,
                        @RequestParam(value = "keywords", required = false) String keywords,
                        Model model) throws UnsupportedEncodingException {
        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        PostQueryCondition condition = new PostQueryCondition();
        condition.setPostType(PostTypeEnum.POST_TYPE_POST.getValue());
        condition.setKeywords(keywords);
        Page<Post> postPage = postService.findPostByCondition(condition, page);
        model.addAttribute("page", postPage);

        model.addAttribute("type", "new");
        return "home/index";
    }

    /**
     * 推荐文章
     *
     * @param pageNumber
     * @param pageSize
     * @param sort
     * @param order
     * @param model
     * @return
     */
    @RequestMapping(value = "/post/recommend", method = RequestMethod.GET)
    public String recommend(@RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                            @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                            @RequestParam(value = "sort", defaultValue = "isSticky desc,  postViews") String sort,
                            @RequestParam(value = "order", defaultValue = "desc") String order,
                            Model model) {

        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        PostQueryCondition condition = new PostQueryCondition();
        condition.setIsRecommend(1);
        condition.setPostType(PostTypeEnum.POST_TYPE_POST.getValue());

        Page<Post> postPage = postService.findPostByCondition(condition, page);
        model.addAttribute("page", postPage);

        model.addAttribute("type", "recommend");
        return "home/index";
    }

    /**
     * 热门文章
     *
     * @param pageNumber
     * @param pageSize
     * @param sort
     * @param order
     * @param model
     * @return
     */
    @RequestMapping(value = "/post/hot", method = RequestMethod.GET)
    public String hot(@RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                      @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                      @RequestParam(value = "sort", defaultValue = "isSticky desc,  postViews") String sort,
                      @RequestParam(value = "order", defaultValue = "desc") String order,
                      Model model) {

        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        PostQueryCondition condition = new PostQueryCondition();
        condition.setPostType(PostTypeEnum.POST_TYPE_POST.getValue());

        Page<Post> postPage = postService.findPostByCondition(condition, page);
        model.addAttribute("page", postPage);

        model.addAttribute("type", "hot");
        return "home/index";
    }

    /**
     * 我发布的文章
     *
     * @param pageNumber
     * @param pageSize
     * @param sort
     * @param order
     * @param model
     * @return
     */
    @RequestMapping(value = "/post/publish", method = RequestMethod.GET)
    public String publish(@RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                          @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                          @RequestParam(value = "sort", defaultValue = "commentSize desc, postViews") String sort,
                          @RequestParam(value = "order", defaultValue = "desc") String order,
                          Model model) {
        Long userId = getLoginUserId();
        if (userId == null) {
            return "redirect:/login";
        }
        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        PostQueryCondition condition = new PostQueryCondition();
        condition.setPostType(PostTypeEnum.POST_TYPE_POST.getValue());

        condition.setUserId(userId);
        Page<Post> postPage = postService.findPostByCondition(condition, page);
        model.addAttribute("page", postPage);

        model.addAttribute("type", "publish");
        return "home/index";
    }


    /**
     * 公告
     *
     * @param pageNumber
     * @param pageSize
     * @param sort
     * @param order
     * @param model
     * @return
     */
    @RequestMapping(value = "/post/notice", method = RequestMethod.GET)
    public String notice(@RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                         @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                         @RequestParam(value = "sort", defaultValue = "createTime") String sort,
                         @RequestParam(value = "order", defaultValue = "desc") String order,
                         Model model) {
        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        QueryCondition queryCondition = new QueryCondition();
        Post condition = new Post();
        condition.setPostType(PostTypeEnum.POST_TYPE_NOTICE.getValue());
        queryCondition.setData(condition);
        Page<Post> postPage = postService.findAll(page, queryCondition);
        for (Post post : postPage.getRecords()) {
            post.setUser(userService.get(post.getUserId()));
        }
        model.addAttribute("page", postPage);
        model.addAttribute("type", "notice");
        return "home/notice";
    }


}
