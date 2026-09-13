/**
 * HorseController — REFERENCE IMPLEMENTATION
 *
 * Đây là ví dụ mẫu về cách tạo REST Controller trong dự án.
 * Convention:
 *  - @RestController + @RequestMapping("/api/{resource}") — tất cả API đều đặt dưới /api/
 *  - Constructor injection (không dùng @Autowired trên field)
 *  - Trả về ApiResponse<T> thay vì trả thẳng entity — giữ nhất quán response format
 *  - KHÔNG xử lý business logic trong Controller — đưa vào Service layer khi logic phức tạp hơn
 */
package com.rtms.backend.controller;

import com.rtms.backend.dto.ApiResponse;
import com.rtms.backend.entity.Horse;
import com.rtms.backend.repository.HorseRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/horses")
public class HorseController {

    private final HorseRepository horseRepository;

    public HorseController(HorseRepository horseRepository) {
        this.horseRepository = horseRepository;
    }

    @GetMapping
    public ApiResponse<List<Horse>> getAllHorses() {
        return ApiResponse.success(horseRepository.findAll());
    }

    @PostMapping
    public ApiResponse<Horse> createHorse(@RequestBody Horse horse) {
        Horse saved = horseRepository.save(horse);
        return ApiResponse.success(saved);
    }

    @GetMapping("/{id}")
    public ApiResponse<Horse> getHorseById(@PathVariable Long id) {
        Horse horse = horseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horse not found with id: " + id));
        return ApiResponse.success(horse);
    }
}