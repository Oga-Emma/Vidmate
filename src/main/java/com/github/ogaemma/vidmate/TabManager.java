package com.github.ogaemma.vidmate;

import com.github.ogaemma.vidmate.model.FileDto;

import java.io.File;
import java.util.List;
import java.util.Set;

interface TabManager {
    void openNewTab(File file);
    void openNewDeepSearchTab(List<File> rootDirectories, String query);
    void updateNewTabName(String name);
}
