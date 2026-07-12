package org.main.account.web;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.main.account.dto.AuthErrorResponse;
import org.main.account.dto.SaveProfileRequest;
import org.main.account.dto.SavedProfileDto;
import org.main.account.dto.UpdateSavedProfileRequest;
import org.main.account.security.AppPrincipal;
import org.main.account.service.SavedProfileService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account/saved-profiles")
public class SavedProfileController {

    private final SavedProfileService savedProfileService;

    public SavedProfileController(SavedProfileService savedProfileService) {
        this.savedProfileService = savedProfileService;
    }

    @GetMapping
    public List<SavedProfileDto> list(@AuthenticationPrincipal AppPrincipal principal) {
        return savedProfileService.list(principal.userId());
    }

    @PostMapping
    public ResponseEntity<SavedProfileDto> save(
            @AuthenticationPrincipal AppPrincipal principal,
            @Valid @RequestBody SaveProfileRequest request
    ) {
        SavedProfileDto saved = savedProfileService.save(
                principal.userId(),
                request.puuid(),
                request.personalLabel()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PatchMapping("/{id}")
    public SavedProfileDto update(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSavedProfileRequest request
    ) {
        return savedProfileService.update(
                principal.userId(),
                id,
                request.personalLabel(),
                request.isDefault()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable UUID id
    ) {
        savedProfileService.delete(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(SavedProfileService.DuplicateSavedProfileException.class)
    public ResponseEntity<AuthErrorResponse> duplicate(
            SavedProfileService.DuplicateSavedProfileException exception
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT).
                body(new AuthErrorResponse(exception.getMessage()));
    }
}
