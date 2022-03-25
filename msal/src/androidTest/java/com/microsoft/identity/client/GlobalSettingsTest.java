package com.microsoft.identity.client;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests for {@link GlobalSettings}.
 */
@RunWith(AndroidJUnit4.class)
public class GlobalSettingsTest {
    private Context mAppContext;

    @Before
    public void setup() {

    }

    @Test
    public void testCanCreatePCAWithoutGlobalInit() {

    }

    @Test
    public void testCannotInitGlobalIfPcaHasBeenCreated() {

    }

    @Test
    public void testCanCreatePcaAfterGlobalInit() {

    }

    @Test
    public void testGlobalFieldsOverridePCAFields() {

    }

    // TODO: Once we figure out which fields should be global-only or pca-specific-only, add tests to make sure they can't be read in through other methods.
}
