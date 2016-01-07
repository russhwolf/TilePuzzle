package io.intrepid.russell.tilepuzzle;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
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

    public static final String EXTRA_IMAGE_RESOURCE = "image_resource";
    public static final String EXTRA_SIZE = "size";

    private RecyclerView mTileGrid;
    private TextView mStatusView;
    private int mSize;
    private int mImageResource;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tile);

        mImageResource = getIntent().getIntExtra(EXTRA_IMAGE_RESOURCE, R.raw.bruce);
        mSize = getIntent().getIntExtra(EXTRA_SIZE, 4); // If we can't read anything, default is medium

        mTileGrid = (RecyclerView) findViewById(R.id.tile_grid);
        mStatusView = (TextView) findViewById(R.id.status);

        initializeGridAsync();
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
                                initializeGridAsync();
                            }
                        }).create().show();
                break;
            case R.id.menu_exit:
                // TODO clear any saved state once that exists
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initializeGridAsync() {
        new AsyncTask<Integer, Void, TileAdapter>() {

            @Override
            protected TileAdapter doInBackground(Integer... params) {
                int imageResource = params[0];
                int size = params[1];

                // TODO cache (picasso) as in MainActivity
                Bitmap raw = Utils.decodeSampledBitmapFromResource(getResources(), imageResource, 500, 500);
                Bitmap[] tiles = generateTiles(raw, size);
                return new TileAdapter(size, tiles, mStatusView);
            }

            @Override
            protected void onPostExecute(TileAdapter adapter) {
                mTileGrid.setLayoutManager(new GridLayoutManager(TileActivity.this, adapter.mSize));
                mTileGrid.setAdapter(adapter);
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

}

class TileAdapter extends RecyclerView.Adapter<TileAdapter.ViewHolder> {
    private static final String TAG = TileAdapter.class.getSimpleName();

    final int mMissingValue;
    final int mSize;
    final int[] mValues;
    final Bitmap[] mTiles;
    final TextView mStatusView;

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
            int value = mValues[position];
            int size = mSize;

//            Toast.makeText(v.getContext(), "Item "+position+" clicked (value = "+value+")", Toast.LENGTH_SHORT).show();

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

    TileAdapter(int size, Bitmap[] tiles, TextView statusView) {
        setHasStableIds(true); // This gives us animation when moving tiles

        mSize = size;
        mTiles = tiles;
        mValues = new int[size * size];
        for (int i = 0; i < mSize * mSize; i++) {
            mValues[i] = i;
        }

        mMissingValue = size * size - 1;

        mStatusView = statusView;

        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                int seconds = params[0];
                try {
                    Thread.sleep(seconds * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                initializePuzzle();
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                mStarted = true;
                mStatusView.setText(mStatusView.getResources().getQuantityString(R.plurals.moves, mMoves, mMoves));
                notifyDataSetChanged();
            }
        }.execute(1);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        int value = mValues[position];
        if (!mSolved && value == mMissingValue) {
            holder.itemView.setClickable(false);
            holder.image.setImageResource(android.R.color.transparent);
        } else {
            holder.itemView.setClickable(mStarted && !mSolved);
            holder.image.setImageBitmap(mTiles[value]);
        }
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
            checkSolved();
            mMoves++;
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



