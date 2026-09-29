// SPDX-License-Identifier: MIT
package com.shouyun.jiahaomode.quote;
import com.shouyun.jiahaomode.JiahaoMode;
import net.minecraft.util.Identifier;
import java.util.*;
import java.util.function.IntUnaryOperator;
public final class JiahaoQuoteRegistry {
 private static final Map<Identifier,JiahaoQuote> QUOTES = new LinkedHashMap<>();
 private static final Map<JiahaoQuoteCategory,List<JiahaoQuote>> CATEGORIES = new EnumMap<>(JiahaoQuoteCategory.class);
 static {
  for (var category:JiahaoQuoteCategory.values()) {
   var entries=new ArrayList<JiahaoQuote>();
   for(int i=1;i<=category.count;i++) {
    String key=category.name().toLowerCase(Locale.ROOT)+"."+i;
    var quote=new JiahaoQuote(JiahaoMode.id("quote/"+key),"jiahao.quote."+key,category,category.priority,50,Optional.empty(),Optional.empty());
    QUOTES.put(quote.id(),quote);entries.add(quote);
   }
   CATEGORIES.put(category,List.copyOf(entries));
  }
 }
 public static JiahaoQuote get(Identifier id) {return QUOTES.get(id);}
 public static JiahaoQuote select(JiahaoQuoteCategory category,Identifier last,Identifier lastCategory,IntUnaryOperator random) {
  var all=CATEGORIES.get(category);
  var candidates=all.stream().filter(q->!q.id().equals(last)&&!q.id().equals(lastCategory)).toList();
  if(candidates.isEmpty()) candidates=all;
  return candidates.get(random.applyAsInt(candidates.size()));
 }
 private JiahaoQuoteRegistry() {}
}
