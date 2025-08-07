package org.fergs.objects;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public final class SearchResult {
    public final String title, url, thumbnail;
    public SearchResult(String t, String u, String th) {
        title = t; url = u; thumbnail = th;
    }
}
