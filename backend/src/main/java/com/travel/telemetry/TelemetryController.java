package com.travel.telemetry;

import com.travel.common.exception.*;
import com.travel.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.*;

@RestController
@RequestMapping("/api/v1/admin/ai/executions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') and hasAuthority('admin:system:read')")
public class TelemetryController {
    private final MongoExecutionStore store;
    private final ExecutionOutbox outbox;
    private final Clock clock;
    public record Response(String status,Instant asOf,int days,int page,int size,int retentionDays,ExecutionOutbox.State sync,MongoExecutionStore.Data data){}
    @GetMapping
    public ApiResponse<Response> list(@RequestParam(defaultValue="30")int days,@RequestParam(defaultValue="0")int page){
        if(!java.util.Set.of(7,30,90).contains(days)||page<0||page>1000000)throw new AppException(ErrorCode.VALIDATION_ERROR);
        var sync=outbox.state();
        if(!store.enabled())return ApiResponse.success(new Response("DISABLED",clock.instant(),days,page,20,store.retentionDays(),sync,null));
        try{return ApiResponse.success(new Response("AVAILABLE",clock.instant(),days,page,20,store.retentionDays(),sync,store.read(days,page)));}
        catch(RuntimeException unavailable){return ApiResponse.success(new Response("UNAVAILABLE",clock.instant(),days,page,20,store.retentionDays(),sync,null));}
    }
}
