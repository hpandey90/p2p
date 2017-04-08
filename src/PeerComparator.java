import java.util.Comparator;


public class PeerComparator<T extends PeerManager> implements Comparator<PeerManager> {

	@Override
	public int compare(PeerManager o1, PeerManager o2) {

		return (int)(o1.getPeerDownloadRate() - o2.getPeerDownloadRate()); // since it's a min heap
	}


}
