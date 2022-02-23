package com.microsoft.identity.client;

import android.app.Activity;
import com.microsoft.identity.client.exception.MsalException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.junit.Assert;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class SignInParametersTest {

    @Before
    public void setup() {
        // None
    }

    @Test
    public void testWithScope() {
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(new Activity())
                .withScope("a")
                .withCallback(getCallback())
                .build();

        List<String> scopes = new ArrayList<>();
        scopes.add("a");

        Assert.assertEquals(signInParameters.getScopes(), scopes);
    }

    @Test
    public void testWithScopeMultiple() {
        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(new Activity())
                .withScope("a")
                .withScope("b")
                .withCallback(getCallback())
                .build();

        List<String> scopes = new ArrayList<>();
        scopes.add("a");
        scopes.add("b");

        Assert.assertEquals(signInParameters.getScopes(), scopes);
    }

    @Test
    public void testWithScopes() {
        List<String> scopes = new ArrayList<>();
        scopes.add("a");
        scopes.add("b");

        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(new Activity())
                .withScopes(scopes)
                .withCallback(getCallback())
                .build();

        List<String> compareScopes = new ArrayList<>();
        compareScopes.add("a");
        compareScopes.add("b");

        Assert.assertEquals(signInParameters.getScopes(), compareScopes);
    }

    @Test
    public void testWithScopeAndScopes() {
        List<String> scopes = new ArrayList<>();
        scopes.add("a");
        scopes.add("b");

        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(new Activity())
                .withScopes(scopes)
                .withScope("c")
                .withCallback(getCallback())
                .build();

        List<String> compareScopes = new ArrayList<>();
        compareScopes.add("a");
        compareScopes.add("b");
        compareScopes.add("c");

        Assert.assertEquals(signInParameters.getScopes(), compareScopes);
    }

    @Test
    public void testScopeOverride() {
        List<String> scopes = new ArrayList<>();
        scopes.add("a");
        scopes.add("b");

        final SignInParameters signInParameters = SignInParameters.builder()
                .withActivity(new Activity())
                .withScopes(scopes)
                .withScope("c")
                .withCallback(getCallback())
                .build();

        List<String> newScopes = new ArrayList<>();
        newScopes.add("d");
        newScopes.add("e");

        signInParameters.setScopes(newScopes);

        List<String> compareScopes = new ArrayList<>();
        compareScopes.add("d");
        compareScopes.add("e");

        Assert.assertEquals(signInParameters.getScopes(), compareScopes);
    }

    private AuthenticationCallback getCallback() {
        return new AuthenticationCallback() {
            @Override
            public void onCancel() {
                // Nothing
            }
            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                // Nothing
            }
            @Override
            public void onError(MsalException exception) {
                // Nothing
            }
        };
    }
}
