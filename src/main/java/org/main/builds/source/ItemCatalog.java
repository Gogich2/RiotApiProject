package org.main.builds.source;

public interface ItemCatalog {

    boolean isStartingItem(int itemId);

    boolean isCompletedBoot(int itemId);

    boolean isCompletedCoreItem(int itemId);

    void refresh();
}
