package hypernebulae.loading;

/** Tiny 2D affine transform: x' = a*x + c*y + e, y' = b*x + d*y + f. */
final class Affine {
    float a = 1, b = 0, c = 0, d = 1, e = 0, f = 0;

    void translate(float x, float y) { e += a * x + c * y; f += b * x + d * y; }

    void rotateDeg(float deg) {
        float r = (float) Math.toRadians(deg), cs = (float) Math.cos(r), sn = (float) Math.sin(r);
        float nb = b * cs + d * sn;
        float nc = -a * sn + c * cs;
        float nd = -b * sn + d * cs;
        a = a * cs + c * sn; b = nb; c = nc; d = nd;
    }

    void scale(float sx, float sy) { a *= sx; b *= sx; c *= sy; d *= sy; }

    float x(float px, float py) { return a * px + c * py + e; }
    float y(float px, float py) { return b * px + d * py + f; }
}