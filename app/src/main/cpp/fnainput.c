/*
 * Persistent FNADroid uinput helper.
 *
 * Protocol on stdin:
 *   down KEY
 *   up KEY
 *   tap KEY
 *   quit
 *
 * One uinput device is kept alive for the whole controller session.
 * This permits independent simultaneous key states.
 */
#include <linux/uinput.h>
#include <fcntl.h>
#include <unistd.h>
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <errno.h>
#include <strings.h>

static int fd = -1;

static int keycode(const char *k) {
    if (!k) return 0;
    if (!strcasecmp(k,"SPACE")) return KEY_SPACE;
    if (!strcasecmp(k,"ENTER")) return KEY_ENTER;
    if (!strcasecmp(k,"ESC") || !strcasecmp(k,"ESCAPE")) return KEY_ESC;
    if (!strcasecmp(k,"SHIFT")) return KEY_LEFTSHIFT;
    if (!strcasecmp(k,"CTRL")) return KEY_LEFTCTRL;
    if (!strcasecmp(k,"ALT")) return KEY_LEFTALT;
    if (!strcasecmp(k,"TAB")) return KEY_TAB;
    if (!strcasecmp(k,"BACKSPACE")) return KEY_BACKSPACE;
    if (!strcasecmp(k,"UP") || !strcasecmp(k,"DPAD_UP")) return KEY_UP;
    if (!strcasecmp(k,"DOWN") || !strcasecmp(k,"DPAD_DOWN")) return KEY_DOWN;
    if (!strcasecmp(k,"LEFT") || !strcasecmp(k,"DPAD_LEFT")) return KEY_LEFT;
    if (!strcasecmp(k,"RIGHT") || !strcasecmp(k,"DPAD_RIGHT")) return KEY_RIGHT;
    if (!strcasecmp(k,"A")) return KEY_A;
    if (!strcasecmp(k,"B")) return KEY_B;
    if (!strcasecmp(k,"C")) return KEY_C;
    if (!strcasecmp(k,"D")) return KEY_D;
    if (!strcasecmp(k,"E")) return KEY_E;
    if (!strcasecmp(k,"F")) return KEY_F;
    if (!strcasecmp(k,"G")) return KEY_G;
    if (!strcasecmp(k,"H")) return KEY_H;
    if (!strcasecmp(k,"I")) return KEY_I;
    if (!strcasecmp(k,"J")) return KEY_J;
    if (!strcasecmp(k,"K")) return KEY_K;
    if (!strcasecmp(k,"L")) return KEY_L;
    if (!strcasecmp(k,"M")) return KEY_M;
    if (!strcasecmp(k,"N")) return KEY_N;
    if (!strcasecmp(k,"O")) return KEY_O;
    if (!strcasecmp(k,"P")) return KEY_P;
    if (!strcasecmp(k,"Q")) return KEY_Q;
    if (!strcasecmp(k,"R")) return KEY_R;
    if (!strcasecmp(k,"S")) return KEY_S;
    if (!strcasecmp(k,"T")) return KEY_T;
    if (!strcasecmp(k,"U")) return KEY_U;
    if (!strcasecmp(k,"V")) return KEY_V;
    if (!strcasecmp(k,"W")) return KEY_W;
    if (!strcasecmp(k,"X")) return KEY_X;
    if (!strcasecmp(k,"Y")) return KEY_Y;
    if (!strcasecmp(k,"Z")) return KEY_Z;
    return atoi(k);
}

static void emit_key(int code, int value) {
    struct input_event ev;
    memset(&ev, 0, sizeof(ev));
    ev.type = EV_KEY;
    ev.code = code;
    ev.value = value;
    (void)write(fd, &ev, sizeof(ev));
}
static void sync_event(void) {
    struct input_event ev;
    memset(&ev, 0, sizeof(ev));
    ev.type = EV_SYN;
    ev.code = SYN_REPORT;
    ev.value = 0;
    (void)write(fd, &ev, sizeof(ev));
}

static int create_device(void) {
    fd = open("/dev/uinput", O_WRONLY | O_NONBLOCK);
    if (fd < 0) fd = open("/dev/input/uinput", O_WRONLY | O_NONBLOCK);
    if (fd < 0) return -1;

    if (ioctl(fd, UI_SET_EVBIT, EV_KEY) < 0) return -2;
    if (ioctl(fd, UI_SET_EVBIT, EV_SYN) < 0) return -3;

    for (int k = KEY_A; k <= KEY_Z; ++k) ioctl(fd, UI_SET_KEYBIT, k);

    int extra[] = {
        KEY_SPACE, KEY_ENTER, KEY_ESC, KEY_LEFTSHIFT, KEY_LEFTCTRL,
        KEY_LEFTALT, KEY_TAB, KEY_BACKSPACE,
        KEY_UP, KEY_DOWN, KEY_LEFT, KEY_RIGHT
    };
    for (unsigned i = 0; i < sizeof(extra)/sizeof(extra[0]); ++i)
        ioctl(fd, UI_SET_KEYBIT, extra[i]);

    struct uinput_setup us;
    memset(&us, 0, sizeof(us));
    us.id.bustype = BUS_USB;
    us.id.vendor = 0xF0A0;
    us.id.product = 0x0003;
    us.id.version = 1;
    snprintf(us.name, UINPUT_MAX_NAME_SIZE, "FNADroid Virtual Keyboard");

    if (ioctl(fd, UI_DEV_SETUP, &us) < 0) return -4;
    if (ioctl(fd, UI_DEV_CREATE) < 0) return -5;

    usleep(300000);
    return 0;
}

int main(void) {
    if (create_device() != 0) return 10;

    char line[128];
    while (fgets(line, sizeof(line), stdin)) {
        char action[16], key[32];
        memset(action, 0, sizeof(action));
        memset(key, 0, sizeof(key));

        if (sscanf(line, "%15s %31s", action, key) < 1)
            continue;

        if (!strcasecmp(action, "quit")) break;

        int kc = keycode(key);
        if (!kc) continue;

        if (!strcasecmp(action, "down")) {
            emit_key(kc, 1);
            sync_event();
        } else if (!strcasecmp(action, "up")) {
            emit_key(kc, 0);
            sync_event();
        } else if (!strcasecmp(action, "tap")) {
            emit_key(kc, 1);
            sync_event();
            emit_key(kc, 0);
            sync_event();
        }
    }

    ioctl(fd, UI_DEV_DESTROY);
    close(fd);
    return 0;
}
