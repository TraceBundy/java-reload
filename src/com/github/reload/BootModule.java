package com.github.reload;

import javax.inject.Singleton;
import com.github.reload.conf.Configuration;
import com.github.reload.net.connections.ConnectionManager;
import com.github.reload.routing.TopologyPlugin;
import com.github.reload.routing.TopologyPluginFactory;
import dagger.Module;
import dagger.Provides;

@Module(injects = {Configuration.class, Overlay.class, TopologyPlugin.class}, complete = false)
public class BootModule {

	private final Configuration conf;

	public BootModule(Configuration conf) {
		this.conf = conf;
	}

	@Provides
	@Singleton
	Overlay provideOverlay(ConnectionManager connMgr, TopologyPlugin topology) {
		return new Overlay(connMgr, topology);
	}

	@Provides
	@Singleton
	Configuration provideConfiguration() {
		return conf;
	}

	@Provides
	@Singleton
	TopologyPlugin provideTopologyPlugin() {
		return TopologyPluginFactory.newInstance(conf.get(Configuration.TOPOLOGY));
	}
}