package com.clipclap.rego.controller;


import com.clipclap.rego.model.dto.DetailPlanDTO;
import com.clipclap.rego.model.dto.FlightInfo;
import com.clipclap.rego.model.dto.PlannerDTO;
import com.clipclap.rego.model.dto.TouristAttractionDTO;
import com.clipclap.rego.repository.DetailPlanRepository;
import com.clipclap.rego.repository.TouristAttractionRepository;
import com.clipclap.rego.service.DetailPlanService;
import com.clipclap.rego.service.PlannerService;
import com.clipclap.rego.service.TouristAttractionService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/plan")
public class PlannerController {

    private final TouristAttractionService touristAttractionService;
    private final TouristAttractionRepository touristAttractionRepository;
    private final ObjectMapper objectMapper;
    private final DetailPlanRepository detailPlanRepository;
    private final DetailPlanService detailPlanService;
    private final PlannerService plannerService;

    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public String myPlanList(Model model , PlannerDTO plannerDTO ) {

        List<PlannerDTO> planList = plannerService.findByAllId();

        System.out.println(planList);
        model.addAttribute("planList", planList);

        return "plan/planList";
    }

    /* 특정회원의 계획 리스트 --> 마이페이지에서 활용하면 될거같음
    @GetMapping("/list")
    @PreAuthorize("isAuthenticated()")
    public String myPlanList(Model model, Principal principal , PlannerDTO plannerDTO ) {

        if (principal != null){
            List<PlannerDTO> dtos = plannerService.findByUserEmail(principal.getName());

            System.out.println(dtos);
            model.addAttribute("test", dtos);
        } else {
            return "redirect:/login";
        }

        return "plan/planList";
    }*/

    @GetMapping("/add")
    @PreAuthorize("isAuthenticated()")
    public String planAddForm(Model model, Principal principal,
                              PlannerDTO plannerDTO,
                              BindingResult bindingResult) {

        if (principal != null){
            System.out.println(principal.getName());
            plannerDTO.setUserEmail(principal.getName());
        }

        return "/plan/planAdd";
    }

    @PostMapping("/add")
    @PreAuthorize("isAuthenticated()")
    public String myPlanAdd(Model model, Principal principal,
                            PlannerDTO plannerDTO,
                            @ModelAttribute FlightInfo flightInfo) {

        model.addAttribute("FlightInfo", flightInfo);
        plannerDTO.setStartDate(LocalDate.parse(flightInfo.getDepartureDate()));
        plannerDTO.setEndDate(LocalDate.parse(flightInfo.getArrivalDate()));
        model.addAttribute("plannerDTO", plannerDTO);
        return "plan/planAdd";
    }

    @PostMapping("/addValid")
    @PreAuthorize("isAuthenticated()")
    public String myPlanAddValid(Model model, Principal principal,
                            @ModelAttribute @Valid PlannerDTO plannerDTO,
                            BindingResult bindingResult,
                            @ModelAttribute FlightInfo flightInfo) {

        if (bindingResult.hasErrors()) {
            List<ObjectError> errors = bindingResult.getAllErrors();
            for (ObjectError error : errors) {
            }
            return "plan/planAdd";
        }

        if (principal != null){
            plannerDTO.setUserEmail(principal.getName());
        }

        Integer id = plannerService.save(plannerDTO);

        return "redirect:/plan/detail?planId=" + id;
    }

    @GetMapping("/detail")
    public String map(@RequestParam(required = false) Integer planId, Model model) throws JsonProcessingException {
        PlannerDTO plannerDTO = plannerService.findById(planId);
        if (plannerDTO == null){
            return "redirect:/";
        }

        List<TouristAttractionDTO> touristAttractionListAll = touristAttractionService.touristListAll();

        String listAll = objectMapper.writeValueAsString(touristAttractionListAll);

        List<DetailPlanDTO> detailList = detailPlanService.findAllByPlan(planId);

        String detailPlan = objectMapper.writeValueAsString(detailList);

        // 상세플랜 목록
        model.addAttribute("detailPlan" , detailPlan);
        // 전체 관광지 리스트
        model.addAttribute("attractionList" , listAll);
        // 도시 리스트 (검색)
        model.addAttribute("cityList" , touristAttractionRepository.findDistinctCityNames());
        // 현재 사용중인 PK 번호 최대
        model.addAttribute("detailIdMax" , detailPlanService.findMaxDetailPlanIdByPlanId(planId));
        // 이후에 정보를 받아오면 필요없을듯
        model.addAttribute("planID" , planId);
        // 플래너의 시작날짜 (이것도 굳이 필요없을 수도)
        // model.addAttribute("startDate" , plannerService.findStartTimeByPlanId(planId));
        model.addAttribute("startDate" , plannerDTO.getStartDate());

        return "plan/planDetail";
    }

    @GetMapping("/Preview")
    public String planPreview(@RequestParam(required = false) Integer planId, Model model) throws JsonProcessingException {
        PlannerDTO plannerDTO = plannerService.findById(planId);
        if (plannerDTO == null){
            return "redirect:/";
        }

        List<TouristAttractionDTO> touristAttractionListAll = touristAttractionService.touristListAll();

        String listAll = objectMapper.writeValueAsString(touristAttractionListAll);

        List<DetailPlanDTO> detailList = detailPlanService.findByPlanPlanIdOrderByStartTime(planId);
        List<Object[]> test=detailPlanRepository.findCityNameAndImageByPlanId(planId);
        String detailPlan = objectMapper.writeValueAsString(detailList);
        // 상세플랜 목록
        model.addAttribute("detailPlan" , detailPlan);
        model.addAttribute("previewPlan" , objectMapper.writeValueAsString(detailPlanService.findPreview(planId)));
        // 전체 관광지 리스트
        model.addAttribute("attractionList" , listAll);
        // 도시 리스트 (검색)
        model.addAttribute("cityList" , touristAttractionRepository.findDistinctCityNames());
        // 현재 사용중인 PK 번호 최대
        model.addAttribute("detailIdMax" , detailPlanService.findMaxDetailPlanIdByPlanId(planId));
        // 이후에 정보를 받아오면 필요없을듯
        model.addAttribute("planID" , planId);
        // 플래너의 시작날짜 (이것도 굳이 필요없을 수도)
        // model.addAttribute("startDate" , plannerService.findStartTimeByPlanId(planId));
        model.addAttribute("startDate" , plannerDTO.getStartDate());

        model.addAttribute("endDate" , plannerDTO.getEndDate());
        model.addAttribute("userEmail" , plannerDTO.getUserEmail());

        return "plan/planPreview";
    }

    @PostMapping("/saveImage")
    @ResponseBody
    public Map<String, Object> saveImage(@RequestBody Map<String, Object> dataToSend) {

        System.out.println("사진 저장 실행...............");
        Map<String, Object> response = new HashMap<>();
        try {
            String base64Data = ((String) dataToSend.get("imageDataURL")).replace("data:image/png;base64,", "");
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            int planId = Integer.parseInt(String.valueOf(dataToSend.get("planId")));
            String filename = "preview/" + planId + ".png"; // 저장할 이미지 파일 이름
            File imageFile = new File("src/main/resources/static/" + filename);

            String folderPath = "src/main/resources/static/preview";
            Path path = Paths.get(folderPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path); // 폴더가 없으면 생성
            }

            try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                fos.write(imageBytes);
            }
            response.put("success", true);
        } catch (IOException e) {
            response.put("success", false);
        }
        System.out.println("사진 저장 완료...............");
        return response;
    }

}
