package com.travel.trip.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import com.travel.trip.dto.CreateTripRequest;
import com.travel.trip.dto.TripResponses.*;
import com.travel.trip.repository.TripCrudRepository;
import com.travel.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TripCrudService {
    private final TripCrudRepository repository;
    private final ObjectMapper json;
    private final Clock clock;

    @Transactional
    public Created create(CreateTripRequest request) {
        UUID owner = SecurityUtils.getCurrentUserId();
        validateStructure(request);
        String title = request.title().isBlank() ? request.destination().name() : request.title();
        String hash = hash(request,title);
        var existing = repository.byCreateKey(owner, request.clientRequestId());
        if (existing.isPresent()) return replay(existing.get(),hash);
        if (request.startDate().isBefore(TripCalendar.today(clock))) invalid("startDate");
        if (!repository.referencesExist(request)) throw new AppException(ErrorCode.TRIP_INVALID_REFERENCE);
        UUID id = UUID.randomUUID();
        if (!repository.insert(id,owner,request,title,hash,write(request.preferences()))) {
            return replay(repository.byCreateKey(owner,request.clientRequestId()).orElseThrow(),hash);
        }
        repository.initialize(id,request);
        return new Created(read(repository.find(owner,id).orElseThrow(),clock.instant()),true);
    }
    private void validateStructure(CreateTripRequest r) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(r.startDate(),r.endDate()) + 1;
        if (days < 1 || days > 365) invalid("endDate");
        if (new HashSet<>(r.preferences()).size()!=r.preferences().size()) invalid("preferences");
        if (new HashSet<>(r.initialPlaceIds()).size()!=r.initialPlaceIds().size()) invalid("initialPlaceIds");
    }
    private void invalid(String field) {
        throw new AppException(ErrorCode.VALIDATION_ERROR,Map.of(field,"Invalid value"));
    }
    private Created replay(TripCrudRepository.Row row, String hash) {
        if (!Objects.equals(row.hash(),hash)) throw new AppException(ErrorCode.TRIP_REQUEST_CONFLICT);
        if (row.deleted()) throw new AppException(ErrorCode.TRIP_NOT_FOUND);
        return new Created(read(row,clock.instant()),false);
    }
    @Transactional(readOnly=true)
    public Read get(UUID id) {
        return read(repository.find(SecurityUtils.getCurrentUserId(),id)
            .orElseThrow(() -> new AppException(ErrorCode.TRIP_NOT_FOUND)),clock.instant());
    }
    @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Page<Summary> list(int page,int size,String mode,String q,String sort) {
        if (page<0 || size<1 || size>100) invalid("page");
        if (q!=null && q.trim().length()>120) invalid("q");
        if (mode!=null && !Set.of("PLANNING","LIVE","RECAP","CANCELLED").contains(mode)) invalid("mode");
        String[] parts=sort.split(",",-1);
        Map<String,String> columns=Map.of("createdAt","t.created_at","updatedAt","t.updated_at","startDate","t.start_date");
        if (parts.length!=2 || !columns.containsKey(parts[0]) || !Set.of("asc","desc").contains(parts[1])) invalid("sort");
        String order=columns.get(parts[0])+" "+parts[1]+" NULLS LAST,t.id "+parts[1];
        Instant now=clock.instant(); Context context=context(now);
        var filter=repository.filter(SecurityUtils.getCurrentUserId(),mode,q,context.evaluatedDate());
        long total=repository.count(filter);
        var rows=repository.list(filter,order,size,(long)page*size);
        return new Page<>(rows.stream().map(r -> summary(r,context.evaluatedDate())).toList(),page,size,total,
            (total+size-1)/size,context);
    }
    private Context context(Instant now) { return new Context(now,now.atZone(TripCalendar.ZONE).toLocalDate()); }
    private Summary summary(TripCrudRepository.Row r, LocalDate today) {
        int count=TripCalendar.days(r.start(),r.end());
        return new Summary(r.id(),r.title(),new Destination(r.destination(),r.provinceId()),r.start(),r.end(),r.zone(),r.status(),
            TripCalendar.mode(r.status(),r.start(),r.end(),today),new Readiness(count>0,r.stopCount()>0,r.unscheduledCount()),
            count,r.stopCount(),r.memoryCount(),null,r.version(),r.created(),r.updated());
    }
    private Read read(TripCrudRepository.Row r,Instant now) {
        Context context=context(now); Summary s=summary(r,context.evaluatedDate());
        Detail detail=new Detail(s.id(),s.title(),s.destination(),s.startDate(),s.endDate(),s.timeZone(),s.status(),s.mode(),
            s.readiness(),s.dayCount(),s.stopCount(),s.memoryCount(),s.coverImageUrl(),s.version(),s.createdAt(),s.updatedAt(),
            r.budget()==null?null:new Budget(r.budget(),"VND"),preferences(r.preferences()),r.accommodation(),r.notes(),
            repository.days(r.id(),r.start()));
        return new Read(detail,context);
    }
    private List<String> preferences(String value) {
        if (value==null || value.isBlank()) return List.of();
        try { return json.readValue(value,new TypeReference<List<String>>() {}); }
        catch (JsonProcessingException e) { throw new IllegalStateException("Legacy trip preferences require migration",e); }
    }
    private String hash(CreateTripRequest r,String title) {
        Map<String,Object> normalized=new LinkedHashMap<>();
        normalized.put("title",title); normalized.put("destination",r.destination());
        normalized.put("startDate",r.startDate()); normalized.put("endDate",r.endDate()); normalized.put("timeZone",r.timeZone());
        normalized.put("budget",r.budget()==null?null:r.budget().amount().longValueExact());
        normalized.put("preferences",r.preferences().stream().map(Enum::name).sorted().toList());
        normalized.put("accommodation",r.accommodation()); normalized.put("notes",r.notes());
        normalized.put("initialPlaceIds",r.initialPlaceIds().stream().map(UUID::toString).sorted().toList());
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(write(normalized).getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    private String write(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException e) { throw new IllegalStateException(e); }
    }
}
