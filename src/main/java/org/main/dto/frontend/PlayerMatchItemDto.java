package org.main.dto.frontend;

public record PlayerMatchItemDto(
        Integer itemId,
        String itemName,
        String imageUrl,
        Integer itemSlot
) {
}
