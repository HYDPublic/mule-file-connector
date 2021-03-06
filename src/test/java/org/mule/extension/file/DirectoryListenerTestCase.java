/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.file;

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.extension.file.AllureConstants.FileFeature.FILE_EXTENSION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;

import org.mule.extension.file.internal.DirectoryListener;
import org.mule.runtime.api.cluster.ClusterService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.notification.ClusterNodeNotification;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.core.api.lifecycle.PrimaryNodeLifecycleNotificationListener;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;

import javax.inject.Inject;

import io.qameta.allure.Feature;

@Feature(FILE_EXTENSION)
@RunWith(MockitoJUnitRunner.class)
@Ignore("MULE-12731")
public class DirectoryListenerTestCase extends AbstractMuleContextTestCase {

  private DirectoryListener directoryListener;

  @Inject
  private NotificationListenerRegistry listenersRegistry;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private SourceCallback sourceCallback;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private MuleContext mockMuleContext;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ClusterService clusterService;

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    when(mockMuleContext.isPrimaryPollingInstance()).thenReturn(false);
    return singletonMap(OBJECT_MULE_CONTEXT, mockMuleContext);
  }

  @Override
  protected void doSetUp() throws Exception {
    directoryListener = new DirectoryListener();
    directoryListener.setClusterService(clusterService);

    muleContext.getInjector().inject(directoryListener);
  }

  @Override
  protected void doTearDown() throws Exception {
    if (directoryListener.isStarted()) {
      directoryListener.onStop();
    }
  }

  @Test
  public void dontStartInSecondaryNode() throws Exception {
    assertThat(directoryListener.isStarted(), is(false));
  }

  @Test
  public void startIfNodeBecomesPrimary() throws Exception {
    ArgumentCaptor<PrimaryNodeLifecycleNotificationListener> listenerCaptor =
        ArgumentCaptor.forClass(PrimaryNodeLifecycleNotificationListener.class);

    directoryListener.onStart(sourceCallback);

    verify(listenersRegistry).registerListener(listenerCaptor.capture());
    PrimaryNodeLifecycleNotificationListener listener = listenerCaptor.getValue();
    assertThat(listener, is(notNullValue()));

    listener.onNotification(mock(ClusterNodeNotification.class));

    verify(clusterService, times(2)).isPrimaryPollingInstance();

  }

}
