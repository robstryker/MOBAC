package mobac.program;

import mobac.program.interfaces.MapInterface;

public abstract class AbstractAtlasThreadListener implements IAtlasThreadListener {

	@Override
	public void initialize(AtlasThread thread) {
		// TODO Auto-generated method stub

	}

	@Override
	public void criticalError(Throwable t) {
		// TODO Auto-generated method stub

	}

	@Override
	public void downloadAborted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void atlasCreationFinished() {
		// TODO Auto-generated method stub

	}

	@Override
	public void beginMap(MapInterface map) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tileDownloaded(int size) {
		// TODO Auto-generated method stub

	}

	@Override
	public void tileLoadedFromCache(int size) {
		// TODO Auto-generated method stub

	}

	@Override
	public void zoomChanged(int zoom) {
		// TODO Auto-generated method stub

	}

	@Override
	public void downloadJobComplete() {
		// TODO Auto-generated method stub

	}

	@Override
	public void tilesMissing(int tileCount, int missing)
			throws InterruptedException {
		// TODO Auto-generated method stub

	}

}
