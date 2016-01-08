package io.intrepid.russell.tilepuzzle;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class TileActivity extends AppCompatActivity {
    private static final String TAG = TileActivity.class.getSimpleName();

    public static final String PREFS_GAME = "io.intrepid.russell.tilepuzzle.game";
    public static final String KEY_IMAGE_RESOURCE = "image_resource";
    public static final String KEY_SIZE = "size";
    public static final String KEY_VALUES = "values";
    public static final String KEY_VALUES_SIZE = "values_size";
    public static final String KEY_MOVES = "moves";

    private RecyclerView mTileGrid;
    private TileAdapter mTileAdapter;
    private TextView mStatusView;
    private int mSize;
    private int mImageResource;
    private boolean mLaunchWithSaveInstanceState;
    private boolean mLaunchWithSaveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tile);

        mImageResource = getIntent().getIntExtra(KEY_IMAGE_RESOURCE, R.raw.bruce);
        mSize = getIntent().getIntExtra(KEY_SIZE, 4); // If we can't read anything, default is medium

        mTileGrid = (RecyclerView) findViewById(R.id.tile_grid);
        mStatusView = (TextView) findViewById(R.id.status);

        mLaunchWithSaveInstanceState = savedInstanceState != null;
        mLaunchWithSaveData = hasSaveData(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tile_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_reset:
                initializeGridAsync();
                break;
            case R.id.menu_difficulty:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.difficulty)
                        .setItems(R.array.difficulties, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int[] difficultyValues = getResources().getIntArray(R.array.difficulty_values);
                                mSize = difficultyValues[which];
                                PreferenceManager.getDefaultSharedPreferences(TileActivity.this).edit().putInt(Utils.PREF_DIFFICULTY, which).apply();
                                initializeGridAsync();
                            }
                        }).create().show();
                break;
            case android.R.id.home:
            case R.id.menu_exit:
                clearSaveData(this);
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        SharedPreferences.Editor editor = getSaveData(this).edit().clear();
        if (mTileAdapter != null && !mTileAdapter.mSolved) {
            // Only save if we have a puzzle in progress
            editor.putInt(KEY_IMAGE_RESOURCE, mImageResource);
            editor.putInt(KEY_SIZE, mSize);
            if (mTileAdapter != null) {
                editor.putInt(KEY_MOVES, mTileAdapter.mMoves);
                editor.putInt(KEY_VALUES_SIZE, mTileAdapter.mValues.length);
                for (int i = 0; i < mTileAdapter.mValues.length; i++) {
                    editor.putInt(KEY_VALUES + i, mTileAdapter.mValues[i]);
                }
            }
        }
        editor.apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mLaunchWithSaveData && !mLaunchWithSaveInstanceState) {
            // Something is saved, so read data
            SharedPreferences prefs = getSaveData(this);
            mImageResource = prefs.getInt(KEY_IMAGE_RESOURCE, mImageResource);
            mSize = prefs.getInt(KEY_SIZE, mSize);
            int moves = prefs.getInt(KEY_MOVES, 0);
            int[] values = new int[prefs.getInt(KEY_VALUES_SIZE, 0)];
            for (int i = 0; i < values.length; i++) {
                values[i] = prefs.getInt(KEY_VALUES + i, i);
            }
            initializeGridAsync(moves, values);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_IMAGE_RESOURCE, mImageResource);
        outState.putInt(KEY_SIZE, mSize);
        if (mTileAdapter != null) {
            outState.putInt(KEY_MOVES, mTileAdapter.mMoves);
            outState.putIntArray(KEY_VALUES, mTileAdapter.mValues);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mImageResource = savedInstanceState.getInt(KEY_IMAGE_RESOURCE, mImageResource);
        mSize = savedInstanceState.getInt(KEY_SIZE, mSize);
        int moves = savedInstanceState.getInt(KEY_MOVES, 0);
        int[] values = savedInstanceState.getIntArray(KEY_VALUES);
        if (values != null) {
            initializeGridAsync(moves, values);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mLaunchWithSaveData && !mLaunchWithSaveInstanceState) {
            initializeGridAsync();
        }
    }

    @Override
    public void onBackPressed() {
        if (mTileAdapter != null && mTileAdapter.mSolved) {
            // If we finished, we should return to the menu here instead of just exiting.
            startActivity(new Intent(this, MainActivity.class));
        }
        super.onBackPressed();
    }

    private static SharedPreferences getSaveData(Context context) {
        return context.getSharedPreferences(PREFS_GAME, MODE_PRIVATE);
    }

    public static boolean hasSaveData(Context context) {
        return getSaveData(context).contains(KEY_SIZE); // Pick an arbitrary key and check that it's there
    }

    public static void clearSaveData(Context context) {
        getSaveData(context).edit().clear().apply();
    }

    private void animateInit() {
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                int seconds = params[0];
                try {
                    Thread.sleep(seconds * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mTileAdapter.begin();
            }
        }.execute(2);
    }

    private void initializeGridAsync() {
        initializeGridAsync(-1, null);
    }

    private void initializeGridAsync(final int moves, final int[] values) {
        new AsyncTask<Integer, Void, Bitmap[]>() {

            @Override
            protected Bitmap[] doInBackground(Integer... params) {
                int imageResource = params[0];
                int size = params[1];

                // TODO cache (picasso) as in MainActivity (This will fix arbitrary size problem)
                Bitmap raw = Utils.decodeSampledBitmapFromResource(getResources(), imageResource, 500, 500);
                Bitmap[] tiles = generateTiles(raw, size);
                return tiles;
            }

            @Override
            protected void onPostExecute(Bitmap[] tiles) {
                TileAdapter adapter;
                if (moves >= 0) {
                    adapter = new TileAdapter(tiles, moves, values);
                } else {
                    adapter = new TileAdapter(tiles);
                }
                mTileGrid.setLayoutManager(new GridLayoutManager(TileActivity.this, mSize));
                mTileGrid.setAdapter(mTileAdapter = adapter);
                if (moves < 0) {
                    animateInit();
                }
            }
        }.execute(mImageResource, mSize);

    }

    private static Bitmap cropSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == height) return bitmap; // short-circuit if we're already square

        int min = Math.min(width, height);

        return Bitmap.createBitmap(bitmap, (width - min) / 2, (height - min) / 2, min, min);
    }

    private static Bitmap[] generateTiles(Bitmap bitmap, int size) {
        bitmap = cropSquare(bitmap);
        int tileSide = bitmap.getWidth() / size;
        Bitmap[] bitmaps = new Bitmap[size * size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int index = j * size + i % size;
                bitmaps[index] = Bitmap.createBitmap(bitmap, i * tileSide, j * tileSide, tileSide, tileSide);
            }
        }
        return bitmaps;
    }

    class TileAdapter extends RecyclerView.Adapter<TileAdapter.ViewHolder> {

        final int mMissingValue;
        final int[] mValues;
        final Bitmap[] mTiles;

        boolean mStarted = false;
        boolean mSolved = false;
        int mMoves = 0;

        class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            ImageView image;

            ViewHolder(View itemView) {
                super(itemView);
                image = (ImageView) itemView.findViewById(R.id.image);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                int size = mSize;

                // Check if we're next to empty block
                if (position >= size) {
                    // We're not in the first row, so there's a square above;
                    int above = position - size;
                    if (mValues[above] == mMissingValue) {
                        swap(position, above);
                        return;
                    }
                }
                if (position < size * (size - 1)) {
                    // We're not in the last row, so there's a square below;
                    int below = position + size;
                    if (mValues[below] == mMissingValue) {
                        swap(position, below);
                        return;
                    }
                }
                if (position % size > 0) {
                    // We're not in the first column, so there's a square to the left
                    int left = position - 1;
                    if (mValues[left] == mMissingValue) {
                        swap(position, left);
                        return;
                    }
                }
                if (position % size < size - 1) {
                    // We're not in the last column, so there's a square to the right
                    int right = position + 1;
                    if (mValues[right] == mMissingValue) {
                        swap(position, right);
                        return;
                    }
                }

            }
        }

        TileAdapter(Bitmap[] tiles, int moves, int[] values) {
            setHasStableIds(true); // This gives us animation when moving tiles

            mTiles = tiles;
            mValues = new int[mSize * mSize];
            System.arraycopy(values, 0, mValues, 0, mValues.length);
            mMoves = moves;

            mMissingValue = mSize * mSize - 1;

            mStarted = true;
            checkSolved();
            if (mSolved) {
                mStatusView.setText(mStatusView.getResources().getString(R.string.solved, mMoves));
            } else {
                mStatusView.setText(mStatusView.getResources().getQuantityString(R.plurals.moves, mMoves, mMoves));
            }
        }

        TileAdapter(Bitmap[] tiles) {
            setHasStableIds(true); // This gives us animation when moving tiles

            mTiles = tiles;
            mValues = new int[mSize * mSize];
            for (int i = 0; i < mSize * mSize; i++) {
                mValues[i] = i;
            }

            mMissingValue = mSize * mSize - 1;
        }

        private void begin() {
            initializePuzzle();
            mStarted = true;
            mStatusView.setText(mStatusView.getResources().getQuantityString(R.plurals.moves, mMoves, mMoves));
            notifyDataSetChanged();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
            return new ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            int value = mValues[position];
            if (mStarted && !mSolved && value == mMissingValue) {
                holder.itemView.setClickable(false);
                holder.itemView.setBackgroundColor(holder.itemView.getResources().getColor(android.R.color.transparent));
                holder.image.setImageResource(android.R.color.transparent);
            } else {
                holder.itemView.setClickable(mStarted && !mSolved);
                holder.itemView.setBackgroundColor(holder.itemView.getResources().getColor(mStarted && !mSolved ? R.color.tile_border : android.R.color.transparent));
                holder.image.setImageBitmap(mTiles[value]);
            }
            int padding = (mStarted && !mSolved) ? holder.itemView.getResources().getDimensionPixelSize(R.dimen.tile_border_width) : 0;
            holder.itemView.setPadding(padding, padding, padding, padding);
        }

        @Override
        public int getItemCount() {
            return mValues.length;
        }

        @Override
        public long getItemId(int position) {
            return mValues[position];
        }

        private void swap(int position1, int position2) {
            swap(position1, position2, true);
        }

        /**
         * @param click true if this was called from user interaction rather than during initialization
         */
        private void swap(int position1, int position2, boolean click) {
            int value1 = mValues[position1];
            int value2 = mValues[position2];
            mValues[position1] = value2;
            mValues[position2] = value1;
            if (click) {
                notifyDataSetChanged();
                mMoves++;
                checkSolved();
                if (mSolved) {
                    mStatusView.setText(mStatusView.getResources().getString(R.string.solved, mMoves));
                } else {
                    mStatusView.setText(mStatusView.getResources().getQuantityString(R.plurals.moves, mMoves, mMoves));
                }
            }
        }

        private void checkSolved() {
            for (int i = 0; i < mValues.length; i++) {
                int value = mValues[i];
                if (value != i) {
                    return;
                }
            }
            mSolved = true;
            notifyDataSetChanged();
        }

        private void initializePuzzle() {
            mMoves = 0;

            // TODO it would be nice to do this randomly and check if the shuffle is valid
            // However the math for this is a pain. Instead we just reverse order and do
            // one more swap if necessary based on parity.
            int numTiles = mValues.length - 1; // Not including empty!
            for (int i = 0; i < numTiles; i++) {
                int newValue = numTiles - 1 - i;
                if (newValue < 0) {
                    newValue += numTiles;
                }
                mValues[i] = newValue;
            }
            if (mSize % 2 == 0) {
                swap(numTiles - 2, numTiles - 1, false);
            }

        }

    }

}


