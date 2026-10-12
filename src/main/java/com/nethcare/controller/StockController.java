package com.nethcare.controller;

import com.nethcare.exception.BusinessException;
import com.nethcare.model.StockCategory;
import com.nethcare.model.StockItem;
import com.nethcare.service.StockService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * The staff side of the stock catalogue.
 *
 * The public frames page reads the same records, so ordering a frame in here
 * is what puts it on display. Staff update the counts, not patients.
 */
@Controller
public class StockController {

    private final StockService stock;

    public StockController(StockService stock) {
        this.stock = stock;
    }

    @InitBinder("item")
    public void stockFields(WebDataBinder binder) {
        binder.setDisallowedFields("id", "imageName", "reserved", "isActive", "createdAt", "updatedAt");
    }

    private String formError(Authentication auth, StockItem item, Model model, BusinessException error) {
        who(auth, model);
        model.addAttribute("item", item);
        model.addAttribute("categories", StockCategory.values());
        model.addAttribute("active", "stock");
        model.addAttribute("error", error.getMessage());
        return "stock/form";
    }

    private void who(Authentication auth, Model model) {
        model.addAttribute("user", auth == null ? "staff" : auth.getName());
    }

    @GetMapping("/stock")
    public String list(Authentication auth, @RequestParam(name = "category", required = false) String category,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "all") String view,
                       Model model) {
        who(auth, model);

        StockCategory cat = null;
        if (category != null && !category.isBlank()) {
            try {
                cat = StockCategory.valueOf(category.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<StockItem> items;
        if ("low".equals(view)) {
            items = stock.lowStock();
        } else if (cat != null || (q != null && !q.isBlank())) {
            items = stock.search(cat, q);
        } else {
            items = stock.listed();
        }

        model.addAttribute("items", items);
        model.addAttribute("categories", StockCategory.values());
        model.addAttribute("lowCount", stock.lowStock().size());
        model.addAttribute("active", "stock");
        model.addAttribute("view", view);
        model.addAttribute("q", q);
        return "stock/list";
    }

    @GetMapping("/stock/new")
    public String form(Authentication auth, Model model) {
        who(auth, model);
        model.addAttribute("item", new StockItem());
        model.addAttribute("categories", StockCategory.values());
        model.addAttribute("active", "stock");
        return "stock/form";
    }

    @PostMapping("/stock")
    public String save(@ModelAttribute("item") StockItem item,
                       @RequestParam(required = false) MultipartFile photo,
                       Authentication auth, Model model, RedirectAttributes out) {
        try {
            StockItem saved = stock.add(item, photo);
            out.addFlashAttribute("msg", "Added " + saved.getItemCode());
            return "redirect:/stock";
        } catch (BusinessException error) {
            item.setImageName(null);
            return formError(auth, item, model, error);
        }
    }

    @GetMapping("/stock/{id}/edit")
    public String edit(Authentication auth, @PathVariable Long id, Model model) {
        who(auth, model);
        model.addAttribute("item", stock.get(id));
        model.addAttribute("categories", StockCategory.values());
        model.addAttribute("active", "stock");
        return "stock/form";
    }

    @PostMapping("/stock/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("item") StockItem incoming,
                         @RequestParam(required = false) MultipartFile photo,
                         @RequestParam(defaultValue = "false") boolean removePhoto,
                         Authentication auth, Model model, RedirectAttributes out) {
        try {
            StockItem saved = stock.update(id, incoming, photo, removePhoto);
            out.addFlashAttribute("msg", "Updated " + saved.getItemCode());
            return "redirect:/stock";
        } catch (BusinessException error) {
            incoming.setId(id);
            incoming.setImageName(stock.get(id).getImageName());
            return formError(auth, incoming, model, error);
        }
    }

    /**
     * Stock count. Reserved units are left alone — a recount of what is on the
     * shelf must not free stock that a live order is holding.
     */
    @GetMapping("/stock/{id}/count")
    public String countForm(Authentication auth, @PathVariable Long id, Model model) {
        who(auth, model);
        model.addAttribute("item", stock.get(id));
        model.addAttribute("active", "stock");
        return "stock/count";
    }

    @PostMapping("/stock/{id}/count")
    public String count(@PathVariable Long id,
                        @RequestParam int counted,
                        @RequestParam String reason,
                        RedirectAttributes out) {
        StockItem item = stock.adjustQuantity(id, counted, reason);
        out.addFlashAttribute("msg", "Counted " + item.getItemCode() + " at " + item.getQuantity());
        return "redirect:/stock";
    }
}
