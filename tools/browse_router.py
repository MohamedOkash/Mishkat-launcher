import urllib.request
import html
import re

def browse(obj_id):
    soap_action = '"urn:schemas-upnp-org:service:ContentDirectory:1#Browse"'
    soap_body = f'''<?xml version="1.0" encoding="utf-8"?>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/" s:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/">
<s:Body>
<u:Browse xmlns:u="urn:schemas-upnp-org:service:ContentDirectory:1">
<ObjectID>{obj_id}</ObjectID>
<BrowseFlag>BrowseDirectChildren</BrowseFlag>
<Filter>*</Filter>
<StartingIndex>0</StartingIndex>
<RequestedCount>50</RequestedCount>
<SortCriteria></SortCriteria>
</u:Browse>
</s:Body>
</s:Envelope>'''
    req = urllib.request.Request('http://192.168.1.1:52868/web/cds_control', data=soap_body.encode('utf-8'))
    req.add_header('Content-Type', 'text/xml; charset="utf-8"')
    req.add_header('SOAPAction', soap_action)
    with urllib.request.urlopen(req, timeout=3) as r:
        raw = r.read().decode('utf-8', errors='ignore')
        res = re.search(r'<Result>(.*?)</Result>', raw, re.DOTALL)
        if res:
            didl = html.unescape(res.group(1))
            print(f"=== BROWSE ObjectID: '{obj_id}' ===")
            print(didl)
            return didl
    return ""

if __name__ == '__main__':
    browse('0$4$215')
