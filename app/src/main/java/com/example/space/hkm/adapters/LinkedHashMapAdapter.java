package com.example.space.hkm.adapters;


        import android.content.Context;
        import android.view.LayoutInflater;
        import android.view.View;
        import android.view.ViewGroup;
        import android.widget.BaseAdapter;
        import android.widget.Filter;
        import android.widget.Filterable;
        import android.widget.TextView;

        import java.util.ArrayList;
        import java.util.LinkedHashMap;
        import java.util.List;
        import java.util.Map;

public class LinkedHashMapAdapter<T> extends BaseAdapter implements Filterable {

    public enum FilterType {
        WORD_PREFIX,
        ANYWHERE
    }

    private LinkedHashMap<CharSequence, List<T>> originalData = null;
    private List<Entry<T>> flattenedData;

    private LayoutInflater inflator;

    private int separatorRowLayoutId;
    private int separatorRowTextViewId;
    private int elementRowLayoutId;
    private int elementRowTextViewId;

    private Filter filter = null;
    private FilterType filterType = FilterType.WORD_PREFIX;

    private static final int ITEM_VIEW_TYPE_ELEMENT = 0;
    private static final int ITEM_VIEW_TYPE_SEPARATOR = 1;

    public LinkedHashMapAdapter(Context context, LinkedHashMap<CharSequence, List<T>> data) {
        this(context, data, 0, 0, 0, 0);
    }

    public LinkedHashMapAdapter(Context context, LinkedHashMap<CharSequence, List<T>> data,
                                int separatorRowLayoutId, int separatorRowTextViewId,
                                int elementRowLayoutId, int elementRowTextViewId) {
        this.inflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        this.originalData = data;
        this.flattenedData = getFlattenedList(data);

        this.separatorRowLayoutId = separatorRowLayoutId;
        this.separatorRowTextViewId = separatorRowTextViewId;
        this.elementRowLayoutId = elementRowLayoutId;
        this.elementRowTextViewId = elementRowTextViewId;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        return flattenedData.size();
    }

    @Override
    public Entry<T> getItem(int position) {
        return flattenedData.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position).value == null ? ITEM_VIEW_TYPE_SEPARATOR : ITEM_VIEW_TYPE_ELEMENT;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        return getItemViewType(position) != ITEM_VIEW_TYPE_SEPARATOR;
    }

    public View getElementView(int position, View convertView, ViewGroup parent) {
        CharSequence text = getItem(position).value.toString();
        return createViewFromResource(convertView, parent, elementRowLayoutId, elementRowTextViewId, text);
    }

    public View getSeparatorView(int position, View convertView, ViewGroup parent) {
        CharSequence text = getItem(position).key;
        return createViewFromResource(convertView, parent, separatorRowLayoutId, separatorRowTextViewId, text);
    }

    protected View createViewFromResource(View convertView, ViewGroup parent,
                                          int layoutId, int textViewId, CharSequence text) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflator.inflate(layoutId, parent, false);

            holder = new ViewHolder();
            holder.textView = (TextView) convertView.findViewById(textViewId);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // populate subviews for the instance in scope
        holder.textView.setText(text);

        return convertView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (getItemViewType(position) == ITEM_VIEW_TYPE_SEPARATOR) {
            convertView = getSeparatorView(position, convertView, parent);
        } else {
            convertView = getElementView(position, convertView, parent);
        }

        return convertView;
    }

    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
    }

    /**
     * It is easier to work with a flattened list when building individual rows, but because the
     * data can be changed at any time from filtering, we need to reflatten the map on occasion
     *
     * @return a list of key and value pairs, with null values where each section header should go
     */
    private List<Entry<T>> getFlattenedList(LinkedHashMap<CharSequence, List<T>> data) {
        List<Entry<T>> list = new ArrayList<>();
        for (Map.Entry<CharSequence, List<T>> items : data.entrySet()) {
            if (!items.getValue().isEmpty()) {
                // null here signifies a separator row.
                list.add(new Entry<>(items.getKey(), null));
                for (T item : items.getValue()) {
                    list.add(new Entry<>(items.getKey(), item));
                }
            }
        }
        return list;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new LinkedHashMapFilter();
        }
        return filter;
    }

    /**
     * The below was adapted from the ArrayFilter inner class of ArrayAdapter, which is
     * unfortunately private.
     */
    private class LinkedHashMapFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence sequence) {
            FilterResults results;

            LinkedHashMap<CharSequence, List<T>> newData;
            synchronized (this) {
                newData = new LinkedHashMap<>(originalData.size());
                for (CharSequence key : originalData.keySet()) {
                    // Make a copy of each list
                    newData.put(key, new ArrayList<>(originalData.get(key)));
                }
            }

            if (sequence == null || sequence.length() == 0) {
                results = getFilterResults(newData);
            } else {
                String substring = sequence.toString().toLowerCase();

                for (Map.Entry<CharSequence, List<T>> entry : newData.entrySet()) {
                    final List<T> values = entry.getValue();
                    final ArrayList<T> newValues = new ArrayList<>();

                    for (T value : values) {
                        final String valueText = value.toString().toLowerCase();

                        // First match against the whole, non-splitted value
                        if (valueText.startsWith(substring)) {
                            newValues.add(value);
                        } else if (filterType == FilterType.WORD_PREFIX) {
                            final String[] words = valueText.split(" ");

                            // Start at index 0, in case valueText starts with space(s)
                            for (String word : words) {
                                if (word.startsWith(substring)) {
                                    newValues.add(value);
                                    break;
                                }
                            }
                        } else if (filterType == FilterType.ANYWHERE && valueText.contains(substring)) {
                            newValues.add(value);
                        }
                    }
                    newData.put(entry.getKey(), newValues);
                }

                results = getFilterResults(newData);
            }

            return results;
        }

        private FilterResults getFilterResults(LinkedHashMap<CharSequence, List<T>> newData) {
            FilterResults results;
            results = new FilterResults();
            List<Entry<T>> flatList = getFlattenedList(newData);
            results.values = flatList;
            results.count = flatList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            //noinspection unchecked
            flattenedData = (List<Entry<T>>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }

    public static class Entry<T> {
        public final CharSequence key;
        public final T value;

        public Entry(CharSequence key, T value) {
            this.key = key;
            this.value = value;
        }
    }

    private static class ViewHolder {
        TextView textView;
    }
}