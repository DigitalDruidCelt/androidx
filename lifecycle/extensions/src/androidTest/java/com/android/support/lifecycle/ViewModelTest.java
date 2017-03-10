/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.support.lifecycle;

import static com.android.support.lifecycle.Lifecycle.ON_RESUME;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.annotation.UiThreadTest;
import android.support.test.filters.MediumTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.android.support.lifecycle.viewmodeltest.TestViewModel;
import com.android.support.lifecycle.viewmodeltest.ViewModelActivity;
import com.android.support.lifecycle.viewmodeltest.ViewModelActivity.ViewModelFragment;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@MediumTest
@RunWith(AndroidJUnit4.class)
public class ViewModelTest {
    private static final int TIMEOUT = 2; // secs

    @Rule
    public ActivityTestRule<ViewModelActivity> mActivityRule =
            new ActivityTestRule<>(ViewModelActivity.class);

    @Test
    public void ensureSameViewHolders() throws Throwable {
        final TestViewModel[] activityModel = new TestViewModel[1];
        final TestViewModel[] defaultActivityModel = new TestViewModel[1];
        final TestViewModel[] fragment1Model = new TestViewModel[1];
        final TestViewModel[] fragment2Model = new TestViewModel[1];
        final ViewModelActivity[] viewModelActivity = new ViewModelActivity[1];
        viewModelActivity[0] = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewModelFragment fragment1 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_1);
                ViewModelFragment fragment2 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_2);
                assertThat(fragment1, notNullValue());
                assertThat(fragment2, notNullValue());
                assertThat(fragment1.activityModel, is(fragment2.activityModel));
                assertThat(fragment1.fragmentModel, not(is(fragment2.activityModel)));
                assertThat(mActivityRule.getActivity().activityModel, is(fragment1.activityModel));
                activityModel[0] = mActivityRule.getActivity().activityModel;
                defaultActivityModel[0] = mActivityRule.getActivity().defaultActivityModel;
                assertThat(defaultActivityModel[0], not(is(activityModel[0])));
                fragment1Model[0] = fragment1.fragmentModel;
                fragment2Model[0] = fragment2.fragmentModel;
            }
        });
        viewModelActivity[0] = recreateActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ViewModelFragment fragment1 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_1);
                ViewModelFragment fragment2 = getFragment(viewModelActivity[0],
                        ViewModelActivity.FRAGMENT_TAG_2);
                assertThat(fragment1, notNullValue());
                assertThat(fragment2, notNullValue());

                assertThat(fragment1.activityModel, is(activityModel[0]));
                assertThat(fragment2.activityModel, is(activityModel[0]));
                assertThat(fragment1.fragmentModel, is(fragment1Model[0]));
                assertThat(fragment2.fragmentModel, is(fragment2Model[0]));
                assertThat(fragment1.defaultActivityModel, is(defaultActivityModel[0]));
                assertThat(fragment2.defaultActivityModel, is(defaultActivityModel[0]));
                assertThat(mActivityRule.getActivity().activityModel, is(activityModel[0]));
                assertThat(mActivityRule.getActivity().defaultActivityModel,
                        is(defaultActivityModel[0]));
            }
        });
    }

    @Test
    @UiThreadTest
    public void testGetApplication() {
        TestViewModel activityModel = mActivityRule.getActivity().activityModel;
        assertThat(activityModel.getApplication(),
                is(InstrumentationRegistry.getTargetContext().getApplicationContext()));
    }

    @Test
    @UiThreadTest
    public void twoViewModels() throws Throwable {
        ViewModelActivity activity = mActivityRule.getActivity();
        ViewModel1 model1 = ViewModelStore.get(activity, ViewModel1.class);
        ViewModel2 model2 = ViewModelStore.get(activity, ViewModel2.class);
        assertThat(ViewModelStore.get(activity, ViewModel1.class), is(model1));
        assertThat(ViewModelStore.get(activity, ViewModel2.class), is(model2));
    }

    @Test
    @UiThreadTest
    public void twoViewModelsWithSameKey() throws Throwable {
        ViewModelActivity activity = mActivityRule.getActivity();
        String key = "the_key";
        ViewModel1 vm1 = ViewModelStore.get(activity, key, ViewModel1.class);
        assertThat(vm1.mCleared, is(false));
        ViewModel2 vw2 = ViewModelStore.get(activity, key, ViewModel2.class);
        assertThat(vw2, notNullValue());
        assertThat(vm1.mCleared, is(true));
    }

    @Test
    @UiThreadTest
    public void localViewModel() throws Throwable {
        ViewModelActivity activity = mActivityRule.getActivity();
        class VM extends ViewModel1 {
        }
        try {
            ViewModelStore.get(activity, VM.class);
            Assert.fail();
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testOnClear() throws Throwable {
        final ViewModelActivity activity = mActivityRule.getActivity();
        final CountDownLatch latch = new CountDownLatch(1);
        final LifecycleObserver observer = new LifecycleObserver() {
            @SuppressWarnings("unused")
            @OnLifecycleEvent(ON_RESUME)
            void onResume() {
                try {
                    final FragmentManager manager = activity.getSupportFragmentManager();
                    LifecycleFragment fragment = new LifecycleFragment();
                    manager.beginTransaction().add(fragment, "temp").commitNow();
                    ViewModel1 vm = ViewModelStore.get(fragment, ViewModel1.class);
                    assertThat(vm.mCleared, is(false));
                    manager.beginTransaction().remove(fragment).commitNow();
                    assertThat(vm.mCleared, is(true));
                } finally {
                    latch.countDown();
                }
            }
        };

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.getLifecycle().addObserver(observer);
            }
        });
        assertThat(latch.await(TIMEOUT, TimeUnit.SECONDS), is(true));
    }

    public static class ViewModel1 extends ViewModel {
        boolean mCleared = false;

        @Override
        protected void onCleared() {
            mCleared = true;
        }
    }

    public static class ViewModel2 extends ViewModel {
    }

    private ViewModelFragment getFragment(FragmentActivity activity, String tag) {
        return (ViewModelFragment) activity.getSupportFragmentManager()
                .findFragmentByTag(tag);
    }

    private ViewModelActivity recreateActivity() throws Throwable {
        Instrumentation.ActivityMonitor monitor = new Instrumentation.ActivityMonitor(
                ViewModelActivity.class.getCanonicalName(), null, false);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.addMonitor(monitor);
        final ViewModelActivity previous = mActivityRule.getActivity();
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                previous.recreate();
            }
        });
        ViewModelActivity result;

        // this guarantee that we will reinstall monitor between notifications about onDestroy
        // and onCreate
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (monitor) {
            do {
                // the documetation says "Block until an Activity is created
                // that matches this monitor." This statement is true, but there are some other
                // true statements like: "Block until an Activity is destoyed" or
                // "Block until an Activity is resumed"...

                // this call will release synchronization monitor's monitor
                result = (ViewModelActivity) monitor.waitForActivityWithTimeout(4000);
                if (result == null) {
                    throw new RuntimeException("Timeout. Failed to recreate an activity");
                }
            } while (result == previous);
        }
        return result;
    }
}
