package com.example.blog.controller.home;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.controller.common.BaseController;
import com.example.blog.dto.JsonResult;
import com.example.blog.dto.PostQueryCondition;
import com.example.blog.dto.QueryCondition;
import com.example.blog.entity.Follow;
import com.example.blog.entity.Post;
import com.example.blog.entity.User;
import com.example.blog.service.*;
import com.example.blog.util.PageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * @author 言曌
 * @date 2020/3/11 4:59 下午
 */
@Controller
public class FrontUserController extends BaseController {

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private FollowService followService;

    /**
     * 用户列表
     *
     * @param model
     * @return
     */
    @GetMapping("/user")
    public String hotUser(Model model) {
        List<User> users = userService.getHotUsers(100);
        model.addAttribute("users", users);
        return "home/user";
    }

    /**
     * 用户文章列表
     *
     * @param model
     * @return
     */
    @GetMapping("/user/{id}")
    public String index(@PathVariable("id") Long userId,
                        @RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                        @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                        @RequestParam(value = "sort", defaultValue = "createTime") String sort,
                        @RequestParam(value = "order", defaultValue = "desc") String order,
                        Model model) {

        User user = userService.get(userId);
        if (user == null) {
            return renderNotFound();
        }

        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        PostQueryCondition condition = new PostQueryCondition();
        condition.setUserId(userId);
        Page<Post> postPage = postService.findPostByCondition(condition, page);
        model.addAttribute("page", postPage);
        model.addAttribute("user", user);

        // 判断当前用户是否关注
        Long loginUserId = getLoginUserId();
        String followFlag = "N";
        if (loginUserId != null) {
            try {
                Follow follow = followService.findByUserIdAndAcceptUserId(loginUserId, userId);
                if (follow != null) {
                    followFlag = "Y";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        model.addAttribute("followFlag", followFlag);
        return "home/user_post";
    }


    /**
     * 我的关注
     *
     * @param pageNumber
     * @param pageSize
     * @param sort
     * @param order
     * @param model
     * @return
     */
    @RequestMapping(value = "/user/follow", method = RequestMethod.GET)
    public String follow(@RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                         @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                         @RequestParam(value = "sort", defaultValue = "createTime") String sort,
                         @RequestParam(value = "order", defaultValue = "desc") String order,
                         Model model) {

        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        Follow condition = new Follow();
        Long userId = getLoginUserId();
        if (userId == null) {
            return "redirect:/login";
        }
        condition.setUserId(userId);
        Page<Follow> postPage = followService.findAll(page, new QueryCondition<>(condition));
        for (Follow follow : postPage.getRecords()) {
            follow.setIsMutualFollowing(followService.isMutualFollowing(follow.getUserId(), follow.getAcceptUserId()));
            follow.setUser(userService.get(follow.getAcceptUserId()));
        }
        model.addAttribute("follows", postPage.getRecords());
        model.addAttribute("page", postPage);
        model.addAttribute("type", "follow");

        return "home/user_follow";
    }

    /**
     * 我的粉丝
     *
     * @param pageNumber
     * @param pageSize
     * @param sort
     * @param order
     * @param model
     * @return
     */
    @RequestMapping(value = "/user/fans", method = RequestMethod.GET)
    public String fans(@RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                       @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                       @RequestParam(value = "sort", defaultValue = "createTime") String sort,
                       @RequestParam(value = "order", defaultValue = "desc") String order,
                       Model model) {

        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        Follow condition = new Follow();
        Long userId = getLoginUserId();
        if (userId == null) {
            return "redirect:/login";
        }
        condition.setAcceptUserId(userId);
        Page<Follow> postPage = followService.findAll(page, new QueryCondition<>(condition));
        for (Follow follow : postPage.getRecords()) {
            follow.setAcceptUser(userService.get(follow.getUserId()));
            follow.setIsMutualFollowing(followService.isMutualFollowing(follow.getUserId(), follow.getAcceptUserId()));

        }
        model.addAttribute("follows", postPage.getRecords());
        model.addAttribute("page", postPage);
        model.addAttribute("type", "fans");

        return "home/user_fans";
    }


    /**
     * 关注用户
     *
     * @param acceptUserId
     * @return
     */
    @RequestMapping(value = "/user/follow", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult follow(@RequestParam("acceptUserId") Long acceptUserId) {
        User user = getLoginUser();
        if (user == null) {
            return JsonResult.error("请先登录");
        }
        if (Objects.equals(user.getId(), acceptUserId)) {
            return JsonResult.error("不能关注自己哦");
        }

        followService.follow(user, acceptUserId);
        return JsonResult.success();
    }

    /**
     * 取关用户
     *
     * @param acceptUserId
     * @return
     */
    @RequestMapping(value = "/user/unfollow", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult unfollow(@RequestParam("acceptUserId") Long acceptUserId) {
        User user = getLoginUser();
        if (user == null) {
            return JsonResult.error("请先登录");
        }
        followService.unfollow(user, acceptUserId);
        return JsonResult.success();
    }


}
