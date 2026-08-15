package com.aliren.rent.house;

import com.aliren.core.auth.UserContext;
import com.aliren.core.common.ApiResponse;
import com.aliren.rent.house.dto.HouseCreateRequest;
import com.aliren.rent.house.dto.HouseListQuery;
import com.aliren.rent.house.dto.HouseResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/houses")
public class HouseController {

    private final HouseService houseService;

    public HouseController(HouseService houseService) {
        this.houseService = houseService;
    }

    @GetMapping
    public ApiResponse<List<HouseResponse>> list(HouseListQuery query) {
        return ApiResponse.ok(houseService.list(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<HouseResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(houseService.detail(id));
    }

    @PostMapping
    public ApiResponse<Long> publish(@Valid @RequestBody HouseCreateRequest req) {
        Long id = houseService.publish(UserContext.requireUserId(), req);
        return ApiResponse.ok(id);
    }
}
