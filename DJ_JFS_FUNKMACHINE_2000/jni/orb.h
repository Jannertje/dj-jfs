typedef struct orb
{
    char v;
    int pos;
} orb;

typedef struct orblist
{
    orb** at;
    int num;
    double x, y, w, h;
    int K;
} orblist;

orblist* init_orblist(int n, double x, double y, double w, double h);
void free_orblist(orblist* L);
int add_orb(orblist* L, int pos, double x, double y, char v);

void sort_orblist(orblist* L);

