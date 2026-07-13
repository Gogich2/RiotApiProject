package org.main.builds.api;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface BuildAssetRepository {

    Optional<DisplayAsset> findChampion(int championId);

    Map<Integer, DisplayAsset> findChampions(List<Integer> championIds);

    Map<Integer, DisplayAsset> findItems(List<Integer> itemIds);

    Map<Integer, DisplayAsset> findRunes(List<Integer> runeIds);

    Map<Integer, DisplayAsset> findSpells(List<Integer> spellIds);
}
