package GInternational.server.security.jwt;

import GInternational.server.api.dto.LoginRequestDto;
import GInternational.server.api.dto.LoginResponseDto;
import GInternational.server.api.entity.Ip;
import GInternational.server.api.entity.User;
import GInternational.server.api.entity.WhiteIp;
import GInternational.server.api.repository.IpRepository;
import GInternational.server.api.repository.UserRepository;
import GInternational.server.api.repository.WhiteIpRepository;
import GInternational.server.api.service.*;
import GInternational.server.api.vo.AdminEnum;
import GInternational.server.api.vo.ExpRecordEnum;
import GInternational.server.common.ipinfo.service.IpInfoService;
import GInternational.server.security.auth.PrincipalDetails;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.ipinfo.api.model.IPResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Optional;


@RequiredArgsConstructor
public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final IpRepository ipRepository;
    private final IpInfoService ipInfoService;
    private final LoginHistoryService loginHistoryService;
    private final LoginStatisticService loginStatisticService;
    private final AdminLoginHistoryService adminLoginHistoryService;
    private final AmazonLoginHistoryService amazonLoginHistoryService;
    private final WhiteIpRepository whiteIpRepository;
    private final ExpRecordService expRecordService;
    private final LoginInfoService loginInfoService;
    private final LoginSuccessHistoryService loginSuccessHistoryService;


    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        ObjectMapper om = new ObjectMapper();
        LoginRequestDto loginRequestDto = null;

        String ip = ipInfoService.getClientIp(request);
        Optional<WhiteIp> whiteIpOptional = whiteIpRepository.findByWhiteIp(ip);

        // 디바이스 타입 추출
        String userAgentString = request.getHeader("User-Agent");
        String deviceType = ipInfoService.extractDeviceTypeFromUserAgentForAdmin(userAgentString);

        Ip validateCheckIp = ipRepository.findByIpContent(ip);
        try {
            loginRequestDto = om.readValue(request.getInputStream(), LoginRequestDto.class);
        } catch (Exception e) {
            throw new BadCredentialsException("잘못된 로그인 요청입니다.");
        }

        User user = null;
        String countryCode = null;

        if (loginRequestDto != null) {
            user = userRepository.findByUsername(loginRequestDto.getUsername());

            IPResponse ipResponse = ipInfoService.getIpInfo(ip);
            countryCode = ipResponse.getCountryCode();
            if (whiteIpOptional.isEmpty() || user.getRole().equals("ROLE_USER") || user.getRole().equals("ROLE_TEST")) {
                if (user != null) {
                    loginHistoryService.saveLoginHistory(loginRequestDto, ip, ipResponse, user.getNickname(), request, countryCode);
                    amazonLoginHistoryService.saveAmazonLoginHistory(loginRequestDto, ip, user.getNickname());
                } else {
                    loginHistoryService.saveLoginHistory(loginRequestDto, ip, ipResponse, null, request, countryCode);
                    amazonLoginHistoryService.saveAmazonLoginHistory(loginRequestDto, ip, null);
                }
            }

            if (user != null) {
                String role = user.getRole();
                if ("ROLE_USER".equals(role) || ("ROLE_TEST".equals(role)) || "ROLE_ADMIN".equals(role) || "ROLE_MANAGER".equals(role)) {
                    if (user.isDeleted()) {
                        throw new BadCredentialsException("삭제된 유저입니다.");
                    }
                    if ("ROLE_ADMIN".equals(role) || "ROLE_MANAGER".equals(role)) {
                        if (user.getAdminEnum() == AdminEnum.사용불가) {
                            throw new BadCredentialsException("계정이 사용 불가 상태입니다.");
                        }
                        if (user.getApproveIp() == null || !user.getApproveIp().equals(ip)) {
                            throw new BadCredentialsException("승인되지 않은 IP입니다.");
                        }
                    }
                    if (validateCheckIp != null) {
                        throw new BadCredentialsException("접근이 차단된 IP입니다.");
                    }

                    UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                            loginRequestDto.getUsername(),
                            loginRequestDto.getPassword());

                    Authentication authentication = authenticationManager.authenticate(authenticationToken);
                    PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

                    return authentication;
                } else {
                    throw new BadCredentialsException("게스트유저는 로그인 할 수 없습니다.");
                }
            } else {
                adminLoginHistoryService.recordLoginAttempt(loginRequestDto.getUsername(), false, ip, null, countryCode, deviceType);
                throw new BadCredentialsException("유저를 찾을 수 없거나 유효하지 않은 유저 상태입니다.");
            }
        } else {
            throw new BadCredentialsException("잘못된 로그인 요청입니다.");
        }
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        PrincipalDetails principalDetails = (PrincipalDetails) authResult.getPrincipal();


        //로그인 시 방문(로그잇 횟수) 증가
        User user = principalDetails.getUser();

        // IP 정보 조회
        String ip = ipInfoService.getClientIp(request);
        IPResponse ipResponse = ipInfoService.getIpInfo(ip);
        String countryCode = ipResponse.getCountryCode();

        Optional<WhiteIp> whiteIpOptional = whiteIpRepository.findByWhiteIp(ip);

        // 디바이스 타입 추출
        String userAgentString = request.getHeader("User-Agent");
        String deviceType = ipInfoService.extractDeviceTypeFromUserAgentForAdmin(userAgentString);

        String jwtToken = JWT.create()
                .withSubject(principalDetails.getUsername())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + JwtProperties.EXPIRATION_TIME))
                .withClaim("userId",principalDetails.getUser().getId())
                .withClaim("username",principalDetails.getUser().getUsername())
                .sign(Algorithm.HMAC512(JwtProperties.SECRET));

        response.addHeader(JwtProperties.HEADER_STRING,
                JwtProperties.TOKEN_PREFIX+jwtToken);

        LoginResponseDto loginResponseDto = new LoginResponseDto();
        loginResponseDto.setUserId(principalDetails.getUser().getId());
        loginResponseDto.setUsername(principalDetails.getUsername());
        loginResponseDto.setNickname(principalDetails.getUser().getNickname());
        loginResponseDto.setName(principalDetails.getUser().getName());
        loginResponseDto.setLv(principalDetails.getUser().getLv());
        loginResponseDto.setLastAccessedIp(ip);
        loginResponseDto.setRole(principalDetails.getUser().getRole());
        loginResponseDto.setVisitCount(user.getVisitCount());
        loginResponseDto.setVisitLog(LocalDateTime.now());
        if (user.getRole().equals("ROLE_USER")) {
            loginResponseDto.setSportsBalance(user.getWallet().getSportsBalance());
            loginResponseDto.setPoint(user.getWallet().getPoint());
            loginResponseDto.setCasinoBalance(user.getWallet().getCasinoBalance());
            loginResponseDto.setWalletId(user.getWallet().getId());
            loginStatisticService.recordLogin();
        }
        // issuedAt 과의 비교검증을 위해 lastVisit을 저장할 때 나노초 제거 후 저장
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime truncatedToSeconds = now.truncatedTo(ChronoUnit.SECONDS);
        user.setLastVisit(truncatedToSeconds);
        user.setVisitCount(user.getVisitCount() + 1);
        user.setLastAccessedIp(ip);
        user.setLastAccessedDevice(deviceType);
        user.setLastAccessedCountry(countryCode);
        userRepository.save(user);

        if (whiteIpOptional.isEmpty()) {
            adminLoginHistoryService.recordLoginAttempt(user.getUsername(), true, ip, user.getNickname(), countryCode, deviceType);
        }
        if ("ROLE_USER".equals(user.getRole()) || "ROLE_TEST".equals(user.getRole())) {
            loginInfoService.saveLoginInfo(user.getUsername(), user.getNickname(), user.getDistributor(), ip, deviceType);
        }

        expRecordService.recordDailyExp(user.getId(), user.getUsername(), user.getNickname(), 1, ip, ExpRecordEnum.로그인경험치);
        loginSuccessHistoryService.saveLoginHistory(user.getId(), ip, request);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String responseBody = objectMapper.writeValueAsString(loginResponseDto);


        response.setContentType("application/json");
        OutputStream outputStream = response.getOutputStream();
        // Write response body
        outputStream.write(responseBody.getBytes());
        outputStream.flush();
    }
}