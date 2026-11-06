package com.usedcar.web;

import com.usedcar.service.CarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MetaController {

    private final CarService carService;

    public MetaController(CarService carService) {
        this.carService = carService;
    }

    /** フォーム用のメタ情報（区分の選択肢 / 登録済みメーカー・取引先・担当者） */
    @GetMapping("/meta")
    public Map<String, Object> meta() {
        return carService.meta();
    }
}
