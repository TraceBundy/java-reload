package com.github.reload.services.storage;

import javax.inject.Singleton;
import com.github.reload.services.storage.local.DataStorage;
import com.github.reload.services.storage.local.MemoryStorage;
import com.github.reload.services.storage.local.StorageController;
import com.github.reload.services.storage.net.ArrayValue;
import com.github.reload.services.storage.net.ArrayValueSpecifier;
import com.github.reload.services.storage.net.DictionaryValue;
import com.github.reload.services.storage.net.DictionaryValueSpecifier;
import com.github.reload.services.storage.net.SingleValue;
import com.github.reload.services.storage.net.SingleValueSpecifier;
import com.github.reload.services.storage.net.StoreKindSpecifier;
import com.github.reload.services.storage.policies.NodeMatch;
import com.github.reload.services.storage.policies.NodeMatch.NodeRIDGenerator;
import com.github.reload.services.storage.policies.UserMatch;
import com.github.reload.services.storage.policies.UserMatch.UserRIDGenerator;
import dagger.Module;
import dagger.Provides;

@Module(injects = {StorageService.class, StorageController.class,
					PreparedData.class, UserRIDGenerator.class,
					NodeRIDGenerator.class, NodeMatch.class, UserMatch.class,
					NodeMatch.NodeRIDGenerator.class,
					UserMatch.UserRIDGenerator.class, MemoryStorage.class,
					SingleValue.class, ArrayValue.class, DictionaryValue.class,
					NodeMatch.class, UserRIDGenerator.class,
					NodeRIDGenerator.class, SingleValueSpecifier.class,
					ArrayValueSpecifier.class, DictionaryValueSpecifier.class,
					StoreKindSpecifier.class}, complete = false)
public class StorageModule {

	@Provides
	@Singleton
	DataStorage provideDataStorage() {
		return new MemoryStorage();
	}
}
