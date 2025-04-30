@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserServiceClient userServiceClient;
  private final JwtUtils jwtUtils;

  @PostMapping("/login")
  public ResponseEntity<JwtResponse> login(@Valid @RequestBody LoginRequest request) {
    boolean isValid = userServiceClient.validateCredentials(request);

    if (isValid) {
      String role = userServiceClient.getUserRole(request.getEmail());
      String token = jwtUtils.generateToken(request.getEmail(), role);
      return ResponseEntity.ok(new JwtResponse(token));
    }
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
  }
}
