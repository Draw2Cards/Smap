import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.smap.R

class ImageAdapter(
    private val context: Context,
    private val imagePaths: List<String>,
    private val recyclerView: RecyclerView,
    private var isLongPressDetected: Boolean = false,
    private var onCheckboxChecked: ((isChecked: Boolean) -> Unit)? = null
) : RecyclerView.Adapter<ImageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageView = holder.itemView.findViewById<ImageView>(R.id.imageView)
        val imageUri = Uri.parse(imagePaths[position])
        imageView.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(imageUri, "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
        }

        imageView.setOnLongClickListener {
            isLongPressDetected = true
            updateCheckboxVisibility()
            Log.d("ImageAdapter", "Long Click")
            true
        }


        holder.checkBox.visibility = if (isLongPressDetected) View.VISIBLE else View.INVISIBLE

        val imagePath = imagePaths[position]
        // Load the image from the file path using a library like Glide or Picasso
        // Example using Glide:
        Glide.with(holder.itemView)
            .load(imagePath)
            .into(imageView)
    }

    private fun updateCheckboxVisibility() {
        val visibility = if (isLongPressDetected) View.VISIBLE else View.INVISIBLE
        for (i in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(i)
            val viewHolder = recyclerView.getChildViewHolder(child) as ViewHolder
            viewHolder.checkBox.visibility = visibility
        }
    }

    override fun getItemCount() = imagePaths.size

    fun setOnCheckboxCheckedListener(listener: (Boolean) -> Unit) {
        onCheckboxChecked = listener
    }
}