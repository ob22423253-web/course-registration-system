package com.uni.registration.controller;

import com.uni.registration.service.WaitlistService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/waitlist")
public class WaitlistController {

    private final WaitlistService service;

    public WaitlistController(WaitlistService service) {
        this.service = service;
    }

    @GetMapping("/{courseId}/size")
    public Map<String, Object> size(@PathVariable String courseId) {
        return Map.of("courseId", courseId, "size", service.size(courseId));
    }

    @GetMapping("/{courseId}/position/{studentId}")
    public Map<String, Object> position(@PathVariable String courseId, @PathVariable String studentId) {
        return Map.of("position", service.position(courseId, studentId));
    }

    @DeleteMapping("/{courseId}/student/{studentId}")
    public Map<String, Object> cancel(@PathVariable String courseId, @PathVariable String studentId) {
        return Map.of("removed", service.cancel(courseId, studentId));
    }
}
