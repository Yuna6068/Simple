package com.example.blog.controller.home;

import cn.hutool.http.HtmlUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.blog.controller.common.BaseController;
import com.example.blog.dto.JsonResult;
import com.example.blog.dto.QueryCondition;
import com.example.blog.entity.*;
import com.example.blog.enums.PostStatusEnum;
import com.example.blog.exception.MyBusinessException;
import com.example.blog.service.*;
import com.example.blog.util.CommentUtil;
import com.example.blog.util.PageUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @author 言曌
 * @date 2020/3/11 4:59 下午
 */
@Controller
public class FrontPostController extends BaseController {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PostService postService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private TagService tagService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private PostMarkRefService postMarkRefService;

    @Autowired
    private PostLikeRefService postLikeRefService;

    /**
     * 文章点赞列表
     *
     * @param pageNumber
     * @param pageSize
     * @param sort
     * @param order
     * @param model
     * @return
     */
    @GetMapping("/post/like")
    public String like(@RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                       @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                       @RequestParam(value = "sort", defaultValue = "createTime") String sort,
                       @RequestParam(value = "order", defaultValue = "desc") String order,
                       Model model) {

        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        PostLikeRef condition = new PostLikeRef();
        Long userId = getLoginUserId();
        if (userId == null) {
            return "redirect:/login";
        }
        condition.setUserId(userId);
        Page<PostLikeRef> postPage = postLikeRefService.findAll(page, new QueryCondition<>(condition));

        for (PostLikeRef postLikeRef : postPage.getRecords()) {
            postLikeRef.setUser(userService.get(postLikeRef.getUserId()));
            postLikeRef.setPost(postService.get(postLikeRef.getPostId()));
        }
        model.addAttribute("page", postPage);

        model.addAttribute("type", "like");
        return "home/post_like";
    }


    /**
     * 文章收藏列表
     *
     * @param pageNumber
     * @param pageSize
     * @param sort
     * @param order
     * @param model
     * @return
     */
    @GetMapping(value = "/post/mark")
    public String mark(@RequestParam(value = "page", defaultValue = "1") Integer pageNumber,
                       @RequestParam(value = "size", defaultValue = "10") Integer pageSize,
                       @RequestParam(value = "sort", defaultValue = "createTime") String sort,
                       @RequestParam(value = "order", defaultValue = "desc") String order,
                       Model model) {

        Page page = PageUtil.initMpPage(pageNumber, pageSize, sort, order);
        PostMarkRef condition = new PostMarkRef();
        Long userId = getLoginUserId();
        if (userId == null) {
            return "redirect:/login";
        }
        condition.setUserId(userId);
        Page<PostMarkRef> postPage = postMarkRefService.findAll(page, new QueryCondition<>(condition));

        for (PostMarkRef postMarkRef : postPage.getRecords()) {
            Post post = postService.get(postMarkRef.getPostId());
            if (post != null) {
                post.setUser(userService.get(post.getUserId()));
                postMarkRef.setPost(post);
            }
        }
        model.addAttribute("page", postPage);

        model.addAttribute("type", "mark");
        return "home/post_mark";
    }

    /**
     * 文章详情
     *
     * @param id
     * @param model
     * @return
     */
    @GetMapping({"/post/{id}", "/notice/{id}"})
    public String postDetails(@PathVariable("id") Long id, Model model) {
        // 文章
        Post post = postService.get(id);
        if (post == null || !Objects.equals(post.getPostStatus(), PostStatusEnum.PUBLISHED.getCode())) {
            return renderNotFound();
        }
        model.addAttribute("post", post);

        // 作者
        User user = userService.get(post.getUserId());
        model.addAttribute("user", user);

        // 分类
        Category category = categoryService.findByPostId(id);
        model.addAttribute("category", category);

        // 标签列表
        List<Tag> tagList = tagService.findByPostId(id);
        model.addAttribute("tagList", tagList);

        // 评论列表
        List<Comment> commentList = commentService.findByPostId(id);
        model.addAttribute("commentList", CommentUtil.getComments(commentList));

        // 访问量加1
        postService.updatePostView(id);
        return "home/post";
    }


    /**
     * 点赞文章
     *
     * @param postId
     * @return
     */
    @PostMapping("/post/like")
    @ResponseBody
    public JsonResult likePost(@RequestParam("postId") Long postId) {
        User user = getLoginUser();
        if (user == null) {
            return JsonResult.error("请先登录");
        }

        Post post = postService.get(postId);
        if (post == null) {
            return JsonResult.error("文章不存在");
        }

        postService.addLike(postId, user);
        return JsonResult.success();
    }

    /**
     * 点踩文章
     *
     * @param postId
     * @return
     */
    @RequestMapping(method = RequestMethod.POST, value = "/post/dislike")
    @ResponseBody
    public JsonResult disLikePost(@RequestParam("postId") Long postId) {
        User user = getLoginUser();
        if (user == null) {
            return JsonResult.error("请先登录");
        }

        Post post = postService.get(postId);
        if (post == null) {
            return JsonResult.error("文章不存在");
        }

        postService.addDisLike(postId, user);
        return JsonResult.success();
    }

    /**
     * 举报文章
     *
     * @param postId
     * @return
     */
    @PostMapping("/post/report")
    @ResponseBody
    public JsonResult reportPost(@RequestParam("postId") Long postId,
                                 @RequestParam("content") String content) {
        User user = getLoginUser();
        if (user == null) {
            return JsonResult.error("请先登录");
        }

        // 查询待处理的反馈
        reportService.findByUserIdAndStatus(user.getId(), 0);

        Report report = new Report();
        report.setUserId(user.getId());
        report.setPostId(postId);
        if (content.length() > 2000) {
            throw new MyBusinessException("字数太多");
        }
        report.setStatus(0);//待处理
        report.setContent(HtmlUtil.escape(content));
        report.setCreateTime(new Date());
        report.setUpdateTime(new Date());
        report.setCreateBy(user.getUserName());
        report.setUpdateBy(user.getUserName());
        reportService.insert(report);
        return JsonResult.success("反馈成功");
    }


    /**
     * 收藏问题
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/post/mark", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult mark(@RequestParam("id") Long id) {
        User user = getLoginUser();
        if (user == null) {
            return JsonResult.error("请先登录");
        }

        postService.addMark(id, user);
        return JsonResult.success("收藏成功");
    }

    /**
     * 收藏文章
     *
     * @param id
     * @return
     */
    @RequestMapping(value = "/post/unmark", method = RequestMethod.POST)
    @ResponseBody
    public JsonResult unmark(@RequestParam("id") Long id) {
        User user = getLoginUser();
        if (user == null) {
            return JsonResult.error("请先登录");
        }

        postService.deleteMark(id, user);
        return JsonResult.success("取消收藏成功");
    }




}
