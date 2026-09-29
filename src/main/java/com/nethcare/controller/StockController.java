package com.nethcare.controller;

import com.nethcare.model.StockCategory;
import com.nethcare.model.StockItem;
import com.nethcare.service.StockService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
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

    private void who(Authentication auth, Model model) {
        model.addAttribute("user", auth == null ? "staff" : auth.getName());
    }

    @GetMapping("/stock")
    public String list(Authentication auth, @RequestParam(required = false) StockCategory category,
                       @RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "all") String view,
                       Model model) {
        who(auth, model);

        List<StockItem> items;
        if ("low".equals(view)) {
            items = stock.lowStock();
        } else if ("expiring".equals(view)) {
            items = stock.expiringSoon();
        } else if (category != null) {
            items = stock.byCategory(category);
        } else {
            items = stock.listed();
        }

        model.addAttribute("items", items);
        model.addAttribute("categories", StockCategory.values());
        model.addAttribute("lowCount", stock.lowStock().size());
        model.addAttribute("expiringCount", stock.expiringSoon().size());
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
    public String save(StockItem item, RedirectAttributes out) {
        StockItem saved = stock.add(item);
        out.addFlashAttribute("msg", "Added " + saved.getItemCode());
        return "redirect:/stock";
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
    public String update(@PathVariable Long id, StockItem incoming, RedirectAttributes out) {
        StockItem saved = stock.update(id, incoming);
        out.addFlashAttribute("msg", "Updated " + saved.getItemCode());
        return "redirect:/stock";
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
