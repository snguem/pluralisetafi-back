package pluralisconseil.sn.pluralisEtatFin.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserDto;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserService;

import java.security.Key;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;

@RequiredArgsConstructor
@Component
public class UserAuthenticationProvider {

    @Value("${security.jwt.token.secret-key:secret-key}")
    private String secretKey;

//    @Value("${security.jwt.token.time:token_time}")
    private long token_time= 5 * 3600 * 1000;

    private final UserService userService;

    @PostConstruct
    protected void init() {
        // this is to avoid having the raw secret key available in the JVM
//        secretKey = Base64.getEncoder().encodeToString(secretKey.getBytes());
    }

    public String createToken(UserDto user) {
        Date now = new Date();

        SignatureAlgorithm algorithm = SignatureAlgorithm.HS256;
        return Jwts.builder()
                .setSubject(user.getLogin())
                .setIssuedAt(now)
                .setExpiration(new Date(System.currentTimeMillis()+ token_time))
                .signWith(getSignKey(), algorithm)
                .compact();
    }


    private Key getSignKey(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }


    public Authentication validateToken(String token) {
        final String username = extractUsername(token);

        UserDto user = userService.getByLogin(username);

        if (user!=null && !isTokenExpired(token))
            return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        else
            return null;
    }

    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        }
        catch (ExpiredJwtException e) {
            return true;
        }
    }

    public Date extractExpiration(String token) {
        return extractAllClaim(token).getExpiration();
    }


    public String  extractUsername(String token){
        return extractAllClaim(token).getSubject();
    }

    private Claims extractAllClaim(String token){
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
