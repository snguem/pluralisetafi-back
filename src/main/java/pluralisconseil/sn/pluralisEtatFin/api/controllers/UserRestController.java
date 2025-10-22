package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.*;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.LoginDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.Response;
import pluralisconseil.sn.pluralisEtatFin.api.models.TokenReponseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserDto;
import pluralisconseil.sn.pluralisEtatFin.config.JwtService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserService;

import java.util.Map;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
@CrossOrigin("*")
public class UserRestController {
    private final UserService service;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper mapper;


    @Operation(summary = "Authentification", description = "Cet uri prend le login et mot de passe de l'utilisateur et l'authentifie")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping("/login")
    public Response<Object> authenticate(@RequestBody LoginDto loginUserDto) {
        Authentication authenticate = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginUserDto.getEmail(), loginUserDto.getPassword()));
        Map<Object, Object> response;
        if(authenticate.isAuthenticated()){
            //Generer le token
            String token= jwtService.createToken(loginUserDto.getEmail());
            TokenReponseDto tokenDto = TokenReponseDto.builder()
                    .token(token)
                    .username(loginUserDto.getEmail())
                    .roles(authenticate.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                    .build();
            return  Response.ok().setPayload(tokenDto).setMessage("Utilisateur authentifie");
        }else{
            return  Response.notFound().setMessage("Utilisateur non trouve");
        }
    }


    @Operation(summary = "Creer un utilisateur", description = "Cet uri prend un utilisateur et le sauvegarde")
    @ApiResponses(value = {@ApiResponse(responseCode = "201", description = "Success"), @ApiResponse(responseCode = "400", description = "Request sent by the client was syntactically incorrect"), @ApiResponse(responseCode = "500", description = "Internal server error during request processing")})
    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Response<Object> createUser(@RequestBody UserDto userDto) {
        try {
            if (userDto.getRoles_string()==null || userDto.getRoles_string().isEmpty()){
                return Response.invalideRoles().setMessage("Renseigner au moins un role");
            }
            var dto = service.create(userDto);
            if (dto==null)
                return Response.invalideRoles().setPayload(userDto.getRoles_string().toString()).setMessage("Roles incorrects");
            return Response.ok().setPayload(dto).setMessage("User créé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Modifier un utilisateur", description = "Cet uri prend un utilisateur et modifi sa reference dans la base de donnees")
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> updateUser(@Parameter(name = "id", description = "l'id de l'utilisateur a mettre a jour") @PathVariable("id") Long id, @RequestBody UserDto userDto) {
        userDto.setId(id);
        try {
            var dto = service.update(userDto);
            return Response.ok().setPayload(dto).setMessage("User modifié");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recuperer un utilisateur", description = "Cet uri permet de recuperer un utilisateur, il prend un id en parametre")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getUser(@Parameter @PathVariable Long id) {
        try {
            var dto = service.get(id);
            return Response.ok().setPayload(dto).setMessage("User trouvé");
        } catch (Exception ex) {
            return Response.badRequest().setMessage(ex.getMessage());
        }
    }

    @Operation(summary = "Recupere l'utilisateur connecte")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @GetMapping("/me")
    public Response<Object> getMe() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            var me = service.getByLogin(authentication.getName());

//            User currentUser = (User) authentication.getPrincipal();
            return Response.ok().setPayload(me).setMessage("Utilisateur connecte");
        }catch (Exception ex){
            return Response.exception().setMessage(ex);
        }
    }


    @Operation(summary = "Lister tout les utilisateurs", description = "Cet uri prend des parametres de filtre et de page et retourn la page correspondante")
    @ApiResponses(value = {@ApiResponse(responseCode = "200"), @ApiResponse(responseCode = "500")})
    @GetMapping("/all")
    @ResponseStatus(HttpStatus.OK)
    public Response<Object> getAllUsers(@RequestParam Map<String, String> searchParams, Pageable pageable) {
        var page = service.getAll(searchParams, pageable);
        Response.PageMetadata metadata = Response.PageMetadata.builder().number(page.getNumber()).totalElements(page.getTotalElements()).size(page.getSize()).totalPages(page.getTotalPages()).build();
        return Response.ok().setPayload(page.getContent()).setMetadata(metadata);
    }

    @Operation(summary = "Supprimer un utilisateur", description = "Cet uri supprimer l'utilisateur correspondant a l'id passer")
    @ApiResponses(value = {@ApiResponse(responseCode = "204"), @ApiResponse(responseCode = "400"), @ApiResponse(responseCode = "404"), @ApiResponse(responseCode = "500")})
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable("id") Long id) {
        try {
            UserDto dto = service.get(id);
            service.delete(id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
