package org.odk.collect.android.storage.migration;

import org.javarosa.core.reference.ReferenceManager;
import org.junit.Before;
import org.junit.Test;
import org.odk.collect.android.analytics.Analytics;
import org.odk.collect.android.preferences.GeneralSharedPreferences;
import org.odk.collect.android.storage.StoragePathProvider;
import org.odk.collect.android.storage.StorageStateProvider;
import org.odk.collect.android.tasks.ServerPollingJob;
import org.odk.collect.android.upload.AutoSendWorker;
import org.odk.collect.utilities.BackgroundWorkManager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.odk.collect.android.preferences.GeneralKeys.KEY_REFERENCE_LAYER;

@SuppressWarnings("PMD.DoNotHardCodeSDCard")
public class StorageMigratorTest {

    private StorageMigrator storageMigrator;
    private final StoragePathProvider storagePathProvider = spy(StoragePathProvider.class);
    private final StorageStateProvider storageStateProvider = mock(StorageStateProvider.class);
    private final StorageEraser storageEraser = mock(StorageEraser.class);
    private final StorageMigrationRepository storageMigrationRepository = mock(StorageMigrationRepository.class);
    private final GeneralSharedPreferences generalSharedPreferences = mock(GeneralSharedPreferences.class);
    private final ReferenceManager referenceManager = mock(ReferenceManager.class);
    private final BackgroundWorkManager backgroundWorkManager = mock(BackgroundWorkManager.class);
    private final Analytics analytics = mock(Analytics.class);

    @Before
    public void setup() {
        whenFormDownloaderIsNotRunning();
        whenFormUploaderIsNotRunning();

        doNothing().when(storageEraser).clearOdkDirOnScopedStorage();
        doNothing().when(storageEraser).deleteOdkDirFromUnscopedStorage();
        doReturn("/sdcard/odk/layers/countries/countries-raster.mbtiles").when(generalSharedPreferences).get(KEY_REFERENCE_LAYER);

        storageMigrator = spy(new StorageMigrator(storagePathProvider, storageStateProvider, storageEraser, storageMigrationRepository, generalSharedPreferences, referenceManager, backgroundWorkManager, analytics));

        doNothing().when(storageMigrator).reopenDatabases();
    }

    @Test
    public void when_formUploaderIsRunning_should_appropriateResultBeReturned() {
        whenFormUploaderIsRunning();
        assertThat(storageMigrator.migrate(), is(StorageMigrationResult.FORM_UPLOADER_IS_RUNNING));
    }

    @Test
    public void when_formDownloaderIsRunning_should_appropriateResultBeReturned() {
        whenFormDownloaderIsRunning();

        assertThat(storageMigrator.migrate(), is(StorageMigrationResult.FORM_DOWNLOADER_IS_RUNNING));
    }

    @Test
    public void when_thereIsNoEnoughSpaceToPerformMigration_should_appropriateResultBeReturned() {
        doReturn(false).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);

        assertThat(storageMigrator.migrate(), is(StorageMigrationResult.NOT_ENOUGH_SPACE));
    }

    @Test
    public void when_anyExceptionIsThrownDuringMovingFiles_should_appropriateResultBeReturned() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(false).when(storageMigrator).moveAppDataToScopedStorage();

        assertThat(storageMigrator.migrate(), is(StorageMigrationResult.MOVING_FILES_FAILED));
    }

    @Test
    public void when_anyExceptionIsThrownDuringMigratingDatabases_should_appropriateResultBeReturned() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(true).when(storageMigrator).moveAppDataToScopedStorage();
        doReturn(false).when(storageMigrator).migrateDatabasePaths();

        assertThat(storageMigrator.migrate(), is(StorageMigrationResult.MOVING_FILES_FAILED));
    }

    @Test
    public void when_migrationFinishedWithSuccess_should_appropriateResultBeReturned() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(true).when(storageMigrator).moveAppDataToScopedStorage();
        doReturn(true).when(storageMigrator).migrateDatabasePaths();

        assertThat(storageMigrator.migrate(), is(StorageMigrationResult.SUCCESS));
    }

    @Test
    public void when_migrationStarts_should_scopedStorageBeCleared() {
        storageMigrator.performStorageMigration();

        verify(storageEraser).clearOdkDirOnScopedStorage();
    }

    @Test
    public void when_movingFilesIsFinished_should_scopedStorageBeEnabled() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(true).when(storageMigrator).moveAppDataToScopedStorage();
        doReturn(false).when(storageMigrator).migrateDatabasePaths();

        storageMigrator.performStorageMigration();

        verify(storageStateProvider).enableUsingScopedStorage();
    }

    @Test
    public void when_movingFilesIsFinished_should_databasesBeReopened() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(true).when(storageMigrator).moveAppDataToScopedStorage();
        doReturn(true).when(storageMigrator).migrateDatabasePaths();

        storageMigrator.performStorageMigration();

        verify(storageMigrator).reopenDatabases();
    }

    @Test
    public void when_movingFilesFailed_should_databasesBeReopenedAgain() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(true).when(storageMigrator).moveAppDataToScopedStorage();
        doReturn(false).when(storageMigrator).migrateDatabasePaths();

        storageMigrator.performStorageMigration();

        verify(storageMigrator, times(2)).reopenDatabases();
    }

    @Test
    public void when_movingFilesFailed_should_scopedStorageBeDisabled() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(true).when(storageMigrator).moveAppDataToScopedStorage();
        doReturn(false).when(storageMigrator).migrateDatabasePaths();

        storageMigrator.performStorageMigration();

        verify(storageStateProvider).disableUsingScopedStorage();
    }

    @Test
    public void when_migrationFinished_should_offlineMapLayerBeUpdated() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(true).when(storageMigrator).moveAppDataToScopedStorage();
        doReturn(true).when(storageMigrator).migrateDatabasePaths();

        storageMigrator.performStorageMigration();

        verify(generalSharedPreferences).save(KEY_REFERENCE_LAYER, "countries/countries-raster.mbtiles");
    }

    @Test
    public void when_migrationFinished_should_referenceManagerBeClearedBeUpdated() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(true).when(storageMigrator).moveAppDataToScopedStorage();
        doReturn(true).when(storageMigrator).migrateDatabasePaths();

        storageMigrator.performStorageMigration();

        verify(referenceManager).reset();
    }

    @Test
    public void when_migrationFinished_should_oldOdkDirBeRemoved() {
        doReturn(true).when(storageStateProvider).isEnoughSpaceToPerformMigration(storagePathProvider);
        doReturn(true).when(storageMigrator).moveAppDataToScopedStorage();
        doReturn(true).when(storageMigrator).migrateDatabasePaths();

        storageMigrator.performStorageMigration();

        verify(storageEraser).deleteOdkDirFromUnscopedStorage();
    }

    private void whenFormDownloaderIsNotRunning() {
        when(backgroundWorkManager.isRunning(ServerPollingJob.TAG)).thenReturn(false);
    }

    private void whenFormDownloaderIsRunning() {
        when(backgroundWorkManager.isRunning(ServerPollingJob.TAG)).thenReturn(true);
    }

    private void whenFormUploaderIsNotRunning() {
        when(backgroundWorkManager.isRunning(AutoSendWorker.TAG)).thenReturn(false);
    }

    private void whenFormUploaderIsRunning() {
        when(backgroundWorkManager.isRunning(AutoSendWorker.TAG)).thenReturn(true);
    }
}
