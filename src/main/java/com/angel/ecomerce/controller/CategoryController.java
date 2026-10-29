package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.Category;
import com.angel.ecomerce.service.category.ICategory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;


@RestController
@RequestMapping("/category")
public class CategoryController {
    private final ICategory iCategory;

    public CategoryController(ICategory iCategory) {
        this.iCategory = iCategory;
    }

    @GetMapping("/heartbeat")
    public String heartbeat(){
        return "alive";
    }

    @GetMapping("/all")
    public List<Category> getAll(){
        return iCategory.getAll();
    }

    @PostMapping("/save")
    public Optional<Category> save(@RequestBody Category category){
        return Optional.ofNullable(iCategory.save(category));
    }

    @DeleteMapping("/delete-all")
    public List<Category> deleteAll(){
        return iCategory.deleteAll();
    }

    @DeleteMapping("/delete-by-id/{id}")
    public String deleteById(@PathVariable Long id){
        return iCategory.deleteById(id);
    }

    @GetMapping("/find-by-id")
    public Optional<Category> findById(@RequestParam Long id){
        return iCategory.findById(id);
    }

    @PutMapping("/update")
    public Optional<Category> updateById(@RequestParam Long id, @RequestBody Category updatedCategory){
        return iCategory.updateById(id,updatedCategory);
    }
}
