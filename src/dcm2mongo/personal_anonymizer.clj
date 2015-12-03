(ns dcm2mongo.personal-anonymizer)
;todo
(defn- anonymize-do-z
  "Z - 長さをゼロにして値をセットしない、あるいは、ゼロでない長さのダミーの値と置換する。値の形式はVRと一致させる。"
  [orgin-value]
  (prn orgin-value)
  (if (seq? orgin-value)                                    ;TODO map?
    orgin-value
    orgin-value
    )
  )
;todo
(defn- anonymize-do-x
  "X - データ要素を削除する。"
  [orgin-value]
  (prn orgin-value)
  (if (seq? orgin-value)                                    ;TODO map?
    orgin-value
    orgin-value
    )
  )
;todo
(defn- anonymize-do-u
  "インスタンスとして一貫性のあるUIDに置き換える。
  ここでの一貫性とは、例えば同じ検査のデータの場合、Study Instance UIDが同じに保つことである。
  利活用目的によっては一貫性が必須のケースがあるため、注意が必要である。"
  [orgin-value]
  (prn orgin-value)
  (if (seq? orgin-value)                                    ;TODO map?
    orgin-value
    orgin-value
    )
  )

(def anonymize-tag
  {:00100020 anonymize-do-z                                 ;患者のID
   :00100010 anonymize-do-z                                 ;患者の名前
   :00100030 anonymize-do-z                                 ;患者の生年月日
   :00101010 anonymize-do-x                                 ;患者の年齢
   :00100040 anonymize-do-x                                 ;患者の性別
   :00080020 anonymize-do-z                                 ;検査日付
   :00080050 anonymize-do-z                                 ;受付番号
   :00200010 anonymize-do-z                                 ;検査ID
   :00081030 anonymize-do-x                                 ;検査内容
   :00104000 anonymize-do-x                                 ;患者コメント
   :00324000 anonymize-do-x                                 ;検査コメント
   :00184000 anonymize-do-x                                 ;収集コメント
   :60xx3000 anonymize-do-x                                 ;オーバーレイデータ
   :00080080 anonymize-do-z                                 ;施設名 x,z,d
   :00081010 anonymize-do-z                                 ;装置名前 x,z,d
   :00081070 anonymize-do-z                                 ;操作社名 x,z,d
   :0020000D anonymize-do-u                                 ;検査インスタンスUID
   :00080018 anonymize-do-u                                 ;SOP インスタンスUID
   }
  )
(def anonymize-did-tag
  {
   :00120062 "YES"                                          ;患者識別削除
   :00120064 113108                                         ;http://dicom.nema.org/dicom/2013/output/chtml/part16/sect_CID_7050.html 匿名化方法コード
   }
  )
;todo;プライベート属性は x の処理を実施
(defn- dissoc-private [obj]
  obj
  )
(defn- anonymize-tag-data [obj]
  (reduce
    (fn [m k]
      (if-let [v (get m k nil)]
        (assoc-in m [k :Value] ((get anonymize-tag k) (get-in m [k :Value])))
        m))
    obj
    (keys anonymize-tag))
  )

(defn attrbute-anonymization [jsonObj]
  (doto jsonObj
    (dissoc-private)
    (anonymize-tag-data)
    (merge anonymize-did-tag)))

