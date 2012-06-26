#include <stdlib.h>
#include <stdio.h>

#include "orb.h"


orblist* init_orblist(int n, double x, double y, double w, double h)
{
    orblist* L = (orblist*) malloc(sizeof(orblist));

    L->at = (orb**) malloc(n * sizeof(orb*));
    L->num = n;
    L->x = x;
    L->y = y;
    L->w = w;
    L->h = h;
    L->K = 1;
    while (L->K * L->K < n)
    {
        L->K += 1;
    }

    return L;
}

void free_orblist(orblist* L)
{
    int i;
    for (i = 0; i < L->num; i++)
    {
        free(L->at[i]);
    }
    free(L->at);
    free(L);
}

int add_orb(orblist* L, int pos, double x, double y, char v)
{
    if (pos < 0 || pos >= L->num)
        return -1;
    if (x < L->x || y < L->y || x >= L->x + L->w || y >= L->y + L->h)
    {
    	x = L->x;
    	y = L->y;
    }

    L->at[pos] = (orb*) malloc(sizeof(orb));

    L->at[pos]->v = v;
    L->at[pos]->pos = (int) ((y - L->y) * L->K / L->h) * L->K
               + (int) ((x - L->x) * L->K / L->w);

    return 0;
}

void sort_orblist(orblist* L)
{
    int i, j;
    orb* A;
    for (i = 1; i < L->num; i++)
    {
        A = L->at[i];
        j = i;
        while ((j > 0) && ((L->at[j-1]->pos - A->pos) > 0))
        {
            L->at[j] = L->at[j-1];
            j -= 1;
        }
        L->at[j] = A;
    }
}
