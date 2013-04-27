/**
 * 
 */
package com.android.ganjoor;

import android.graphics.Point;

/**
 * @author Hamid Reza
 * جهت نگهداری تاریخچه حرکات کاربر و عملیاتی کردن دکمۀ برگشت به کار می رود
 */
public class GanjoorBrowsingHistory {
    public int _CatID;
    public int _CatPageStart;
    public int _PoemID;
    public String _SearchPhrase;
    public int _PoetID;
    public int _SearchStart;
    public int _PageItemsCount;
    public Boolean _FavsPage;
    public Point _AutoScrollPosition;

    public GanjoorBrowsingHistory(int CatID, int PoemID, int CatPageStart, Point AutoScrollPosition)
    {
        _CatID = CatID;
        _PoemID = PoemID;
        _CatPageStart = CatPageStart;
        _AutoScrollPosition = AutoScrollPosition;
    }
    public GanjoorBrowsingHistory(String SearchPhrase, int PoetID, int SearchStart, int PageItemsCount, Boolean FavsPage, Point AutoScrollPosition)
    {
        _CatID = _PoemID = 0;
        _SearchPhrase = SearchPhrase;
        _SearchStart = SearchStart;
        _PageItemsCount = PageItemsCount;
        _FavsPage = FavsPage;
        _PoetID = PoetID;
        _AutoScrollPosition = AutoScrollPosition;
    }
}
