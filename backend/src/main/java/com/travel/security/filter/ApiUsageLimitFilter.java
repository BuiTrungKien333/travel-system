package com.travel.security.filter;

import com.travel.common.exception.AppException;
import com.travel.common.exception.ErrorCode;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import java.io.IOException;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

/** Per-instance budget; shared deployments should enforce the same limits at their gateway. */
@Component
public class ApiUsageLimitFilter extends OncePerRequestFilter {
    private final Clock clock;
    private final HandlerExceptionResolver resolver;
    private final Map<String, Window> windows = new HashMap<>();
    private long cleanedMinute = -1;
    private record Window(long minute, int count) { }
    public ApiUsageLimitFilter(Clock clock, @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.clock=clock;this.resolver=resolver;
    }
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path=request.getRequestURI();
        return !("GET".equals(request.getMethod()) && (path.equals("/api/v1/destinations") || path.startsWith("/api/v1/public/places")
                || path.startsWith("/api/v1/public/weather/")))
                && !path.equals("/api/v1/places/search") && !path.equals("/api/v1/places/addresses")
                && !path.equals("/api/v1/routes") && !path.startsWith("/api/v1/public/planning/")
                && !path.startsWith("/api/v1/planning/");
    }
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        boolean member=auth!=null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");
        String key=member ? "user:"+auth.getName() : "ip:"+request.getRemoteAddr();
        int limit=member ? 180 : 120;
        long minute=clock.instant().getEpochSecond()/60;
        if (!consume(key,limit,minute)) {
            response.setHeader("Retry-After",Long.toString(60-clock.instant().getEpochSecond()%60));
            resolver.resolveException(request,response,null,new AppException(ErrorCode.REQUEST_RATE_LIMITED));
            return;
        }
        chain.doFilter(request,response);
    }
    private synchronized boolean consume(String key,int limit,long minute) {
        if(cleanedMinute!=minute) { windows.values().removeIf(w->w.minute()!=minute);cleanedMinute=minute; }
        var previous=windows.get(key);
        if(previous==null && windows.size()>=10000) return false;
        int count=previous==null ? 0 : previous.count();
        if(count>=limit) return false;
        windows.put(key,new Window(minute,count+1));return true;
    }
}
