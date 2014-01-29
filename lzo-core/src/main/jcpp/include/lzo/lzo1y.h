/* Memory required for the wrkmem parameter.
 * When the required size is 0, you can also pass a NULL pointer.
 */

#define LZO1Y_MEM_COMPRESS      ((lzo_uint32) (16384L * lzo_sizeof_dict_t))
#define LZO1Y_MEM_DECOMPRESS    (0)
#define LZO1Y_MEM_OPTIMIZE      (0)

