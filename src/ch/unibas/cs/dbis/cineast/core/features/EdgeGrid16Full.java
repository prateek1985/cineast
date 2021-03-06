package ch.unibas.cs.dbis.cineast.core.features;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ch.unibas.cs.dbis.cineast.core.config.Config;
import ch.unibas.cs.dbis.cineast.core.data.FloatVector;
import ch.unibas.cs.dbis.cineast.core.data.FloatVectorImpl;
import ch.unibas.cs.dbis.cineast.core.data.Frame;
import ch.unibas.cs.dbis.cineast.core.data.FrameContainer;
import ch.unibas.cs.dbis.cineast.core.data.LongDoublePair;
import ch.unibas.cs.dbis.cineast.core.data.MultiImage;
import ch.unibas.cs.dbis.cineast.core.data.StatElement;
import ch.unibas.cs.dbis.cineast.core.descriptor.EdgeImg;
import ch.unibas.cs.dbis.cineast.core.features.abstracts.AbstractFeatureModule;
import ch.unibas.cs.dbis.cineast.core.util.GridPartitioner;

public class EdgeGrid16Full extends AbstractFeatureModule {

	private static final Logger LOGGER = LogManager.getLogger();

	public EdgeGrid16Full(){
		super("features.EdgeGrid16Full", "grid", 124f / 4f);
	}
	
	
	@Override
	public void processShot(FrameContainer shot) {
		LOGGER.entry();
		if (!phandler.check("SELECT * FROM features.EdgeGrid16Full WHERE shotid = " + shot.getId())) {
			StatElement[] stats = new StatElement[256];
			for(int i = 0; i < 256; ++i){
				stats[i] = new StatElement();
			}
			List<Frame> frames = shot.getFrames();
			List<Boolean> edgePixels = new ArrayList<>();
			for(Frame f : frames){
				MultiImage img = f.getImage();
				edgePixels = EdgeImg.getEdgePixels(img, edgePixels);
				ArrayList<LinkedList<Boolean>> partition = GridPartitioner.partition(edgePixels, img.getWidth(), img.getHeight(), 16, 16);
				for(int i = 0; i < partition.size(); ++i){
					LinkedList<Boolean> edge = partition.get(i);
					StatElement stat = stats[i];
					for(boolean b : edge){
						stat.add(b ? 1 : 0);
					}
				}
			}
			float[] result = new float[64];
			for(int i = 0; i < 64; ++i){
				result[i] = stats[i].getAvg();
			}
			addToDB(shot.getId(), new FloatVectorImpl(result));
		}
		LOGGER.exit();
	}

	@Override
	public List<LongDoublePair> getSimilar(FrameContainer qc) {
		int limit = Config.getRetrieverConfig().getMaxResultsPerModule();
		
		FloatVector query = getEdges(qc.getMostRepresentativeFrame().getImage());
		
		ResultSet rset = this.selector.select("SELECT * FROM features.EdgeGrid16Full USING DISTANCE MINKOWSKI(1)(\'" + query.toFeatureString() + "\', grid) ORDER USING DISTANCE LIMIT " + limit);
		return manageResultSet(rset);
	}

	@Override
	public List<LongDoublePair> getSimilar(FrameContainer qc, String resultCacheName) {
		int limit = Config.getRetrieverConfig().getMaxResultsPerModule();
		
		FloatVector query = getEdges(qc.getMostRepresentativeFrame().getImage());
		
		ResultSet rset = this.selector.select(getResultCacheLimitSQL(resultCacheName) + " SELECT * FROM features.EdgeGrid16Full, c WHERE shotid = c.filter USING DISTANCE MINKOWSKI(1)(\'" + query.toFeatureString() + "\', grid) ORDER USING DISTANCE LIMIT " + limit);
		return manageResultSet(rset);
	}


	private static FloatVector getEdges(MultiImage img){
		StatElement[] stats = new StatElement[256];
		for(int i = 0; i < 256; ++i){
			stats[i] = new StatElement();
		}
		List<Boolean> edgePixels = EdgeImg.getEdgePixels(img, new ArrayList<Boolean>(img.getWidth() * img.getHeight()));
		ArrayList<LinkedList<Boolean>> partition = GridPartitioner.partition(edgePixels, img.getWidth(), img.getHeight(), 16, 16);
		for(int i = 0; i < partition.size(); ++i){
			LinkedList<Boolean> edge = partition.get(i);
			StatElement stat = stats[i];
			for(boolean b : edge){
				stat.add(b ? 1 : 0);
			}
		}
		float[] f = new float[256];
		for(int i = 0; i < 256; ++i){
			f[i] = stats[i].getAvg();
		}
		
		return new FloatVectorImpl(f);
	}

}
