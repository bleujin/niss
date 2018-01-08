package net.bleujin.wine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;

public class WineGroup {

	List<Wine> wines = ListUtil.newList() ;
	
	public WineGroup add(Wine w) {
		wines.add(w) ;
		return this ;
	}
	
	public Wine first(String key, String value) {
		for (Wine w : wines) {
			if (StringUtil.equals(w.asString(key), value)) return w ;
		}
		return null ;
	}
	
	
	public int compareValue(Wine self, Wine other, String field) {
		return self.compareValue(other, field) ;
	}
	
	
	public String toString() {
		return wines.toString() ;
	}

	public Wine findByKoName(String koname) {
		for (Wine wine : wines) {
			if (koname.equals(wine.name())) return wine ;
		}
		return Wine.NOTFOUND;
	}

	public List<Wine> similary(final Wine self, final String[] fields, final WeightContext wcontext) {
		List<Wine> result = new ArrayList<>(wines) ;
		result.remove(self) ;
		
		result.sort(new Comparator<Wine>() {
			@Override
			public int compare(Wine w1, Wine w2) {
				int w1sum = 0 ; int w2sum = 0 ;
				
				for (String field : fields) {
					w1sum += (self.compareValue(w1, field) * wcontext.weight(field));
					w2sum += (self.compareValue(w2, field) * wcontext.weight(field));
				}
				return w1sum - w2sum;
			}
		});
		return result ;
	}
}
