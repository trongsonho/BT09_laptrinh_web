package vn.iotstar.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.iotstar.dto.ProductDTO;
import vn.iotstar.security.CustomUserDetails;
import vn.iotstar.service.ProductService;

@Controller
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    private boolean canManageProduct(ProductDTO product, CustomUserDetails currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return true;
        }
        return product.getUserId() != null && product.getUserId().equals(currentUser.getId());
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestParam(defaultValue = "10") int size,
                       Model model,
                       Authentication authentication) {
        model.addAttribute("products", productService.findAll(keyword, page, size));
        model.addAttribute("keyword", keyword);
        model.addAttribute("size", size);
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails user) {
            model.addAttribute("currentUserId", user.getId());
            boolean isAdmin = user.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            model.addAttribute("isAdmin", isAdmin);
        }
        return "products/list";
    }

    @GetMapping("/create")
    public String create(Model model) {
        model.addAttribute("productDTO", new ProductDTO());
        model.addAttribute("mode", "create");
        return "products/form";
    }

    @PostMapping("/create")
    public String create(@Valid @ModelAttribute ProductDTO dto,
                         BindingResult result,
                         @RequestParam(required = false) MultipartFile image,
                         Authentication authentication,
                         Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            model.addAttribute("mode", "create");
            return "products/form";
        }
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        dto.setUserId(user.getId());
        try {
            productService.create(dto, image);
            redirect.addFlashAttribute("success", "Tạo sản phẩm thành công.");
            return "redirect:/products";
        } catch (Exception e) {
            result.reject("product.error", e.getMessage());
            model.addAttribute("mode", "create");
            return "products/form";
        }
    }

    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       Model model,
                       Authentication authentication,
                       RedirectAttributes redirect) {
        ProductDTO product = productService.findById(id);
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        if (!canManageProduct(product, user)) {
            redirect.addFlashAttribute("error", "Bạn không có quyền chỉnh sửa sản phẩm này.");
            return "redirect:/products";
        }
        model.addAttribute("productDTO", product);
        model.addAttribute("mode", "edit");
        return "products/form";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute ProductDTO dto,
                       BindingResult result,
                       @RequestParam(required = false) MultipartFile image,
                       Authentication authentication,
                       Model model,
                       RedirectAttributes redirect) {
        ProductDTO existing = productService.findById(id);
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        if (!canManageProduct(existing, user)) {
            redirect.addFlashAttribute("error", "Bạn không có quyền chỉnh sửa sản phẩm này.");
            return "redirect:/products";
        }

        if (result.hasErrors()) {
            model.addAttribute("mode", "edit");
            return "products/form";
        }
        try {
            productService.update(id, dto, image);
            redirect.addFlashAttribute("success", "Cập nhật sản phẩm thành công.");
            return "redirect:/products";
        } catch (Exception e) {
            result.reject("product.error", e.getMessage());
            model.addAttribute("mode", "edit");
            return "products/form";
        }
    }

    @PostMapping("/delete/{id}")
    public String delete(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirect) {
        ProductDTO product = productService.findById(id);
        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        if (!canManageProduct(product, user)) {
            redirect.addFlashAttribute("error", "Bạn không có quyền xóa sản phẩm này.");
            return "redirect:/products";
        }

        productService.delete(id);
        redirect.addFlashAttribute("success", "Xóa sản phẩm thành công.");
        return "redirect:/products";
    }
}
